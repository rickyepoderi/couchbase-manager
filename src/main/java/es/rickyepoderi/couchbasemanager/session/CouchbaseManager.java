/***
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *    
 * Linking this library statically or dynamically with other modules 
 * is making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *    
 * As a special exception, the copyright holders of this library give 
 * you permission to link this library with independent modules to 
 * produce an executable, regardless of the license terms of these 
 * independent modules, and to copy and distribute the resulting 
 * executable under terms of your choice, provided that you also meet, 
 * for each linked independent module, the terms and conditions of the 
 * license of that module.  An independent module is a module which 
 * is not derived from or based on this library.  If you modify this 
 * library, you may extend this exception to your version of the 
 * library, but you are not obligated to do so.  If you do not wish 
 * to do so, delete this exception statement from your version.
 *
 * Project: github.com/rickyepoderi/couchbase-manager
 * 
 */
package es.rickyepoderi.couchbasemanager.session;

import es.rickyepoderi.couchbasemanager.couchbase.Client;
import es.rickyepoderi.couchbasemanager.couchbase.ClientRequest;
import es.rickyepoderi.couchbasemanager.couchbase.ClientResult;
import es.rickyepoderi.couchbasemanager.couchbase.ExecOnCompletion;
import es.rickyepoderi.couchbasemanager.couchbase.transcoders.AppSerializingTranscoder;
import es.rickyepoderi.couchbasemanager.session.CouchbaseWrapperSession.SessionMemStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardManager;

/**
 *
 * <p>Basic Manager for any couchbase server (compatible?) memory store.  
 * This is the initial implementation of a <a href="http://www.couchbase.com/">couchbase</a>
 * manager for <a href="http://glassfish.java.net/">glassfish</a>. The main 
 * ideas in this implementation are the following:</p>
 * 
 * <ul>
 * <li>The manager tries to load/unload the session every request. The session
 * is loaded when locking and saved to the store when unlocked (so it is
 * supposed that any modification is always done in locked session state). 
 * Now this is changed, it is only done when non-sticky.</li>
 * <li>Sessions are also stored in normal Map of StandardManager but
 * the session is cleared when not used (cleared means attributes are
 * removed). Again only when non-sticky.</li>
 * <li>The majority of this process lays inside MemWrapperSession implementation.
 * This special session clears its content inside unlock methods and loads it
 * from the memory repository in lock ones. In sticky process lock/unlock
 * is locally managed and the session is not read again or locked.</li>
 * <li>MemWrapperSession uses a activity status flag to record any attribute 
 * modification. If the session is dirty it is saved in the external repository, 
 * if accessed just touched. Obviously session is always unlocked if non-sticky.</li>
 * <li>Expiration is managed by the repository too. Although the processExpires
 * of the StandardManager is used. The MemWrapperSession always checks 
 * external repository to check definitely expiration. Trying to minimize
 * external repository access the local expiration is checked before but
 * if locally expired the session must to be re-loaded to check if it still
 * exists there (if it does not exist, it is actually expired)</li>
 * <li>In glassfish the manager needs to extends 
 * <a href="http://java.net/projects/glassfish/sources/svn/content/trunk/main/appserver/web/web-core/src/main/java/org/apache/catalina/session/StandardManager.java">
 * StandardManager</a> cos the check for expired sessions (processExpires method)
 * only is executed in case of this class (see 
 * <a href="http://java.net/projects/glassfish/sources/svn/content/trunk/main/appserver/web/web-core/src/main/java/org/apache/catalina/core/StandardContext.java">
 * StandardContext</a> method backgroundProcess()). It is not sufficient 
 * extending from ManagerBase.</li>
 * <li>Added sticky configuration that avoids locks and reduce the number
 * of operations in a single request. This mode can only be used if sticky
 * access is guaranteed (all request from the same client are redirected
 * to the same application server instance).</li>
 * <li>Added asynchronous calls to couchbase. Now some operations (the ones
 * that are executed at the end of the request life-cycle) are executed 
 * asynchronously. A lot of code was added.</li>
 * <li>Added error management. The main idea is any operation that returns an
 * error or an incorrect status (not found when unlocking for example) marks
 * the session in error state and throws a IllegalStateException (a unstoppable
 * RuntimeException). Cos async calls are done in the background a error status
 * of the session makes the session to be read (compulsory, no matter sticky
 * or non-sticky). This way a couchbase error is managed.</li>
 * </ul>
 * 
 * <p>Restrictions in the implementation:</p>
 *
 * <ul>
 * <li>Only one couchbase client is used for all the sessions. 
 * <a href="http://stackoverflow.com/questions/8683142/memcached-client-opening-closing-and-reusing-connections">
 * Reading information about spymemcached</a> it seems that it supports multi-thread
 * access and there is no need of pooling. For the moment a single connection
 * is used.</li>
 * <li>The single connection seems to support reconnection. Not checked.</li>
 * <li>Right now if session is not modified it should be unlocked and 
 * touched, but now couchbase client needs two methods to do that (touch
 * and unlock). It would be nice if a touchAndUnlock exists.</li>
 * <li>Same way when session is locked cannot be deleted.</li>
 * </ul>
 * 
 * @author ricky
 */
public class CouchbaseManager extends StandardManager {
    
    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(CouchbaseManager.class.getName());
    
    /**
     * Name of the manager
     */
    protected static final String name = "CouchbaseManager";
    
    /**
     * The descriptive information about this implementation.
     */
    private static final String info = "CouchbaseManager / 0.2";
    
    /**
     * spymemcached client to communicate with the memory repository
     */
    protected Client<CouchbaseWrapperSession> client = null;
    
    //
    // PROPERTIES TO BE SET
    //
    
    /**
     * Connection url for the couchbase server
     */
    protected String url = null;
    
    /**
     * bucket name to be used
     */
    protected String bucket = null;
    
    /**
     * username to be used
     */
    protected String username = null;
    
    /**
     * password to be used
     */
    protected String password = null;
    
    /**
     * stickyness of the manager
     */
    protected boolean sticky = false;
    
    /**
     * lockTime to use inside couchbase locks
     */
    protected int lockTime = 30;
    
    /**
     * maximum time to refresh session without saving
     */
    protected long maxAccessTimeNotSaving = 300000L;
    
    /**
     * maximum time for the operation against couchbase to finish
     */
    protected long operationTimeout = 30000L;
    
    /**
     * The serializing transcoder used to save/get objects in couchbase
     */
    protected AppSerializingTranscoder transcoder = null;
    
    /**
     * Number of nodes to persist an operation
     */
    protected PersistTo persistTo = PersistTo.ZERO;
    
    /**
     * Number of nodes to replicate an operation
     */
    protected ReplicateTo replicateTo = ReplicateTo.ZERO;
    
    //
    // CONSTRUCTOR
    //
    
    /**
     * Constructor via URL of the couchbase.
     * @param url The url where couchbase is.
     */
    public CouchbaseManager(String url) {
        super();
        log.log(Level.FINE, "CouchbaseManager.Constructor: init {0}", url);
        this.url = url;
        log.fine("CouchbaseManager.Constructor: exit");
    }
    
    //
    // GETTERS AND SETTERS
    //

    /**
     * Setter for the bucket.
     * @param bucket The bucket name
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * Password to be used in couchbase.
     * @param password The new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * The url to use.
     * @param url The new url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Username to be used in couchbase.
     * @param username The new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Getter to the sticky property.
     * @return the sticky (true is configured as sticky, false as not)
     */
    public boolean isSticky() {
        return sticky;
    }

    /**
     * Setter to the sticky property.
     * @param sticky The new sticky
     */
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    /**
     * Getter for the lock time in couchbase.
     * @return The amount of time in seconds a key is locked
     */
    public int getLockTime() {
        return lockTime;
    }

    /**
     * Setter for the lock time. Remember that in couchbase now there
     * is a maximum limit of 30 seconds.
     * @param lockTime The new time to use in couchbase
     */
    public void setLockTime(int lockTime) {
        this.lockTime = lockTime;
    }

    /**
     * Getter for the time without saving.
     * @return The time to maintain sessions without saving (ms).
     */
    public long getMaxAccessTimeNotSaving() {
        return maxAccessTimeNotSaving;
    }

    /**
     * Setter for the time without saving.
     * @param maxAccessTimeNotSaving The new time in ms.
     */
    public void setMaxAccessTimeNotSaving(long maxAccessTimeNotSaving) {
        this.maxAccessTimeNotSaving = maxAccessTimeNotSaving;
    }

    /**
     * Getter for the operation timeout.
     * @return The current operation timeout
     */
    public long getOperationTimeout() {
        return operationTimeout;
    }

    /**
     * Setter for the operation timeout.
     * @param operationTimeout The new operation timeout
     */
    public void setOperationTimeout(long operationTimeout) {
        this.operationTimeout = operationTimeout;
    }
    
    /**
     * Setter for the transcoder.
     * @param transcoder The new transcoder
     */
    public void setTranscoder(AppSerializingTranscoder transcoder) {
        this.transcoder = transcoder;
    }

    /**
     * Getter for the PersistTo property.
     * @return The number of nodes to persist an operation
     */
    public PersistTo getPersistTo() {
        return persistTo;
    }

    /**
     * Setter for the persistTo property.
     * @param persistTo The new number of nodes to persist
     */
    public void setPersistTo(PersistTo persistTo) {
        this.persistTo = persistTo;
    }

    /**
     * Getter for the replicateTo property.
     * @return The number of nodes to replicate.
     */
    public ReplicateTo getReplicateTo() {
        return replicateTo;
    }

    /**
     * Setter for the replicateTo property.
     * @param replicateTo The number of nodes to replicate an operation.
     */
    public void setReplicateTo(ReplicateTo replicateTo) {
        this.replicateTo = replicateTo;
    }
    
    //
    // MANAGER METHODS (overriden StandardManager)
    //
    
    /**
     * Info for this manager
     * @return Info for this manager
     */
    @Override
    public String getInfo() {
        return info;
    }
    
    /**
     * Return the descriptive short name of this Manager implementation.
     * @return Description of the manager
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Called from the StandardSession code.
     * The session is added to the internal map. The session is transformed
     * into MemWrapperSession if it is called from StandardSession. Method
     * called after create a new session to add it to the Map.
     * @param session The session to add
     */
    @Override
    public void add(Session session) {
        log.log(Level.FINE, "CouchbaseManager.add(Session): init {0}", session.toString());
        super.add(session);
        log.fine("CouchbaseManager.add(Session): exit");
    }

    /**
     * Method that change the sessionId. It seems that it is used when a
     * session is authenticated (the principal is set). The session is deleted
     * in couchbase, then the id is changed normally and the session is added
     * to couchbase with the new id. The session is marked not loaded.
     * @param session The session to change id
     */
    @Override
    public void changeSessionId(Session session) {
        log.log(Level.FINE, "CouchbaseManager.changeSessionId(Session): init {0}", session.toString());
        // delete the previous session from couchbase and add, lock as if new
        synchronized (session) {
            CouchbaseWrapperSession couchSes = (CouchbaseWrapperSession) session;
            boolean locked = couchSes.isForegroundLocked();
            // delete current session from couchbase
            doSessionDelete(couchSes, null);
            // if it is locked => do the unlock to decrease the count
            if (locked) {
                couchSes.unlockForegroundCompletely();
            }
            // perform normal change id (it is removed and added to the sessions map)
            super.changeSessionId(session);
            // add the new id to couchbase but it is NOT_LOADED
            couchSes.setMemStatus(SessionMemStatus.NOT_LOADED);
            doSessionAdd((CouchbaseWrapperSession)session);
            // if locked lock again
            if (locked) {
                session.lockForeground();
            }
        }
        log.log(Level.FINE, "CouchbaseManager.changeSessionId: exit {0}", session.toString());
    }

    /**
     * Create a new empty session. 
     * @return The new session.
     */
    @Override
    public Session createEmptySession() {
        log.log(Level.FINE, "CouchbaseManager.createEmptySession: init");
        return new CouchbaseWrapperSession(this);
        
    }

    /**
     * Creates a new session but not empty. Initial values are set.
     * @return The new session.
     */
    @Override
    public Session createSession() {
        log.fine("CouchbaseManager.createSession: init");
        String id = this.generateSessionId();
        Session session = createSessionInternal(id, false, true);
        log.log(Level.FINE, "CouchbaseManager.createSession(): exit {0}", session.toString());
        return session;
    }

    /**
     * Create a filled session with the specified id. The session is initialized
     * with the values.
     * @param id The session id.
     * @return The new session.
     */
    @Override
    public Session createSession(String id) {
        log.log(Level.FINE, "CouchbaseManager.createSession(String): init {0}", id);
        Session session = createSessionInternal(id, false, true);
        log.log(Level.FINE, "CouchbaseManager.createSession(String): exit {0}", session.toString());
        return session;
    }
    
    /**
     * Internal method that creates a new session initialized. The session
     * is initialized with some values (timestamps and so on).
     * @param id The session identifier.
     * @param fake The session is fake created, a fake session is not added 
     *        to the manager and nor taken into account (sessionCounter).
     * @param lock lock the session if is a normal (non-fake) one. It is 
     *        only used when a session is found in couchbase but locked.
     *        The session will be locked later.
     * @return The new session.
     */
    public Session createSessionInternal(String id, boolean fake, boolean lock) {
        log.log(Level.FINE, "CouchbaseManager.createSessionInternal(string): init {0} fake={1} lock={2}", 
                new Object[]{id, fake, lock});
        // Recycle or create a Session instance
        CouchbaseWrapperSession session = fake? 
                 new CouchbaseWrapperSession(this, id) : (CouchbaseWrapperSession) createEmptySession();
        // Initialize the properties of the new session and return it
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(this.maxInactiveInterval);
        if (!fake) {
            // real session => set id normally and add the session to couchbase
            session.setId(id);
            // add session to both internal and external
            doSessionAdd(session);
            if (lock) {
                // lock is not done when creating because locked in Couchbase
                session.lockForeground();
            }
            sessionCounter++;
        }
        log.log(Level.FINE, "CouchbaseManager.createSessionInternal(String): exit {0}", session.toString());
        return (session);
    }

    /**
     * Finds a session in the store. This method first check if the session is
     * internally in the map. If it exists in the internal map this session is 
     * returned (initially the session is NOT_LOADED). If it does not exist in
     * the map the method searches for the session in the couchbase server 
     * (maybe the session was created by another server). If it is found
     * this session is returned. Just null is returned if the session does not
     * exist in the couchbase server. Take into account the session is always
     * NOT_LOADED (until later lock).
     * 
     * @param id The session id to find.
     * @return The session found (internal or external) or null
     * @throws IOException Some error finding the session
     */
    @Override
    public Session findSession(String id) throws IOException {
        log.log(Level.FINE, "CouchbaseManager.findSession(String): init {0}", id);
        CouchbaseWrapperSession session = (CouchbaseWrapperSession) super.findSession(id);
        if (session == null) {
            // search for the session in repository with a faked session
            session = (CouchbaseWrapperSession) createSessionInternal(id, true, false);
            this.doSessionLoad(session, SessionMemStatus.NOT_LOADED);
            if (session.getMemStatus().isSuccess()) {
                // add the session to current sessions in this manager 
                // increment the counter
                session.setManager(this);
                sessionCounter++;
                add(session);
            } else if (SessionMemStatus.ALREADY_LOCKED.equals(session.getMemStatus())) {
                // the session exists but it is blocked 
                // just create as new and wait to lock to get read and filled
                // no lock cos we are creating it but it exists earlier
                session = (CouchbaseWrapperSession) createSessionInternal(id, false, false);
                session.setNew(false);
            } else  {
                // session does not exists => null
                session = null;
            }
        } else if (CouchbaseWrapperSession.SessionMemStatus.NOT_EXISTS.equals(session.getMemStatus())) {
            // session does not exists
            log.fine("CouchbaseManager.findSession(String): session is being deleted (NOT_EXISTS)");
            session = null;
        }
        log.log(Level.FINE, "CouchbaseManager.findSession(String): exit {0}", 
                (session == null)? null:session.toString());
        return session;
    }

    /**
     * No version managed, just like previous function.
     * 
     * @param id The session id to find.
     * @param version The version to find.
     * @return The session found (internal or external) or null
     * @throws IOException Some error finding the session
     */
    @Override
    public Session findSession(String id, String version) throws IOException {
        log.log(Level.FINE, "CouchbaseManager.findSession(String,String): init {0} {1}", new Object[] {id, version});
        return findSession(id);
    }

    /**
     * Find session from the request, again same method.
     * 
     * @param id The session id to find.
     * @param request The request.
     * @return The session found (internal or external) or null
     * @throws IOException Some error finding the session
     */
    @Override
    public Session findSession(String id, HttpServletRequest request) throws IOException {
        log.log(Level.FINE, "CouchbaseManager.findSession(String,Request): init {0} {1}", new Object[] {id, request});
        return findSession(id);
    }
        
    public void realRemove(Session session) {
        log.log(Level.FINE, "CouchbaseManager.realRemove(Session): init {0}", session.toString());
        super.remove(session);
        log.fine("CouchbaseManager.realRemove(Session): exit");
    }
    
    /**
     * Method that is called from StandardSession inside expire method.
     * The session is removed from the internal map and from the external 
     * repository (if not already in NOT_EXISTS state).
     * @param session The session to remove
     */
    @Override
    public void remove(Session session) {
        log.log(Level.FINE, "CouchbaseManager.remove(Session): init {0}", session.toString());
        synchronized (session) {
            if (SessionMemStatus.NOT_EXISTS.equals(((CouchbaseWrapperSession) session).getMemStatus())) {
                log.fine("CouchbaseManager.remove(Session): real removing");
                realRemove(session);
            } else {
                doSessionDelete((CouchbaseWrapperSession) session,
                        new OperationComplete((CouchbaseWrapperSession) session));
            }
        }
        log.fine("CouchbaseManager.remove(Session): exit");
    }

    /**
     * Expires a session, just calling StandardManager super method.
     * 
     * @param id The session id to expire.
     */
    @Override
    public void expireSession(String id) {
        log.log(Level.FINE, "CouchbaseManager.expireSession(String): init {0}", id);
        super.expireSession(id);
        log.fine("CouchbaseManager.expireSession(String): exit");
    }
    
    //
    // Methods that I don't know if they have to been implemented
    //

    @Override
    public void update(HttpSession session) throws Exception {
        log.log(Level.FINE, "CouchbaseManager.update(Session): init/exit {0}", session.toString());
    }

    @Override
    public void preRequestDispatcherProcess(ServletRequest request, ServletResponse response) {
        log.log(Level.FINE, "CouchbaseManager.preRequestDispatcherProcess(Request,Response): init/exit {0} {1}", 
                new Object[] {request, response});
    }

    @Override
    public void postRequestDispatcherProcess(ServletRequest request, ServletResponse response) {
        log.log(Level.FINE, "CouchbaseManager.postRequestDispatcherProcess(Request,Response): init/exit {0} {1}",
                new Object[]{request, response});
    }
    
    //
    // OVERWRITEN METHOD: just empty (better to no create problems with StandardManager implementation)
    //

    @Override
    public void load() throws ClassNotFoundException, IOException {
        // noop
    }

    @Override
    public void unload() throws IOException {
        // noop
    }

    @Override
    public void clearStore() {
        // noop
    }

    @Override
    public void release() {
        // noop
    }

    @Override
    public void readSessions(InputStream is) throws ClassNotFoundException, IOException {
        // noop
    }

    @Override
    public void writeSessions(OutputStream os) throws IOException {
        // noop
    }
    
    //
    // INIT METHODS
    //
    
    /**
     * Initializes the couchbase manager.
     */
    @Override
    public synchronized void init() {
        log.fine("CouchbaseManager.init: init");
        super.init();
        try {
            // start the memcached client
            ArrayList baseURIs = new ArrayList();
            String[] uris = this.url.split(",");
            for (String uri: uris) {
                uri = uri.trim();
                baseURIs.add(new URI(uri));
            }
            //client = new CouchbaseClient(baseURIs, "default", null, "");
            transcoder.setAppLoader(this.getContainer().getLoader().getClassLoader());
            client = new Client<CouchbaseWrapperSession>(baseURIs, this.bucket, 
                    this.username, this.password, transcoder, persistTo, replicateTo);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error initiliazing spymemcached client...", e);
            initialized = false;
        }
        log.fine("CouchbaseManager.init: exit");
    }

    /**
     * Destroy the manager. Just shutdown the client connection.
     */
    @Override
    public void destroy() {
        log.fine("CouchbaseManager.destroy: init");
        super.destroy();
        // stop the spymemcached client
        client.shutdown();
        log.fine("CouchbaseManager.destroy: exit");
    }

    /**
     * Stop the manager without deleting the sessions (the sessions also are
     * in the couchbase and they should not be deleted). Even is not shutdown
     * (removement of the application)???
     * @param isShutdown it marks if the server is being shutdown
     * @throws LifecycleException  Some error.
     */
    @Override
    public void stop(boolean isShutdown) throws LifecycleException {
        log.log(Level.FINE, "CouchbaseManager.stop: init {0}", isShutdown);
        // do not delete sessions on stop, sessions deleted against couchbase too
        // I am not removing them even in undeployment
        clearSessions();
        super.stop(isShutdown);
        log.fine("CouchbaseManager.stop: exit");
    }
    
    //
    // Extra method to save session inside the valve
    //
    
    /**
     * Loads a new session from the couchbase repository. The session is 
     * read using gets or getAndLock (sticky or non-sticky configuration). 
     * The session is filled with the read data using fill method.
     * @param session The session to load or refresh (it is modified)
     * @param expected The result that is wanted (NOT_LOADED, FOREGROUND_LOCK, BACKGROUND_LOCK)
     * @return The same session re-loaded
     */
    public CouchbaseWrapperSession doSessionLoad(CouchbaseWrapperSession session, SessionMemStatus expected) {
        log.log(Level.FINE, "CouchbaseManager.doSessionLoad(Session,SessionMemStatus): init {0} {1}", 
                new Object[]{session.toString(), expected});
        // wait previous execution if exists
        session.waitOnExecution();
        if (expected.equals(session.getMemStatus()) && !session.localHasExpired()) {
            // during the waiting another thread has achieved the desired state
            log.fine("The session is already at the desired state");
            return session;
        } else if (!SessionMemStatus.ERROR.equals(session.getMemStatus()) && this.isSticky()
                && !session.localHasExpired()) {
            // no error, no local expiry and sticky => return just like this
            log.fine("Sticky access => not need to load the session in couchbase");
            session.setCas(-1);
            session.setMemStatus(expected);
        } else {
            // if non-sticky or there's an error in background operation => force a read from couchbase
            ClientRequest req;
            if (expected.isLocked() && !this.isSticky()) {
                req = client.getAndLock(session, this.lockTime);
            } else {
                req = client.gets(session);
            }
            ClientResult res = client.waitForCompletion(req, this.operationTimeout);
            if (res.isSuccess()) {
                log.fine("The session was in the repository, returning it");
                CouchbaseWrapperSession loaded = (CouchbaseWrapperSession) res.getValue();
                session.fill(loaded, expected, res.getCas());
            } else if (res.isNotFound()) {
                // TODO: I don't know why sometimes "NOT_FOUND" and "Not Found"
                //       is returned. Maybe I should try to check the result 
                //       better or using other way.
                log.fine("NOT_FOUND => session doesn't exist in the repo");
                session.setCas(-1);
                session.setMemStatus(SessionMemStatus.NOT_EXISTS);
            } else if (res.isLockError()) {
                log.fine("LOCK_ERROR => session exists but not read");
                session.setCas(-1);
                session.setMemStatus(SessionMemStatus.ALREADY_LOCKED);
            } else {
                session.setCas(-1);
                session.setMemStatus(SessionMemStatus.ERROR);
                IllegalStateException e = new IllegalStateException(res.getStatus().getMessage(), res.getException());
                log.log(Level.SEVERE, "Error loading from the repo.", e);
                throw e;
            }
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionLoad(Session,SessionMemStatus): exit {0}", session.toString());
        return session;
    }
    
    /**
     * Performs a session save using the CAS. The session is saved and touched
     * (expiration is refreshed), the method uses secure CAS method to prevent
     * multi-access errors.
     * TODO: cas has no exp method without transcoder.
     * @param session The session to save.
     * @param exec If not null the method is executed asynchronously
     * @return the request executing if exec is not null or null is not async
     */
    public ClientRequest doSessionSave(CouchbaseWrapperSession session, ExecOnCompletion exec) {
        log.log(Level.FINE, "CouchbaseManager.doSessionSave(Session,ExecOnCompletion): init {0} {1}", 
                new Object[]{session.toString(), exec});
        session.waitOnExecution();
        // save the session inside reposiroty
        ClientRequest req;
        if (isSticky()) {
            req = client.set(session, this.getMaxInactiveInterval());
        } else {
            req = client.cas(session, session.getCas(), this.getMaxInactiveInterval());
        }
        if (exec == null) {
            ClientResult res = client.waitForCompletion(req, this.operationTimeout);
            if (!res.isSuccess()) {
                session.setMemStatus(SessionMemStatus.ERROR);
                IllegalStateException e = new IllegalStateException(res.getStatus().getMessage(), res.getException());
                log.log(Level.SEVERE, "Error saving in the repo.", e);
                throw e;
            }
            req = null;
        } else {
            client.execOnCompletion(req, this.operationTimeout, exec);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionSave(Session,ExecOnCompletion): exit {0}", req);
        return req;
    }
    
    /**
     * Deletes a session from the couchbase server.
     * @param session The session to delete.
     * @param exec If not null the method is executed asynchronously
     * @return the request executing if exec is not null or null is not async
     */
    public ClientRequest doSessionDelete(CouchbaseWrapperSession session, ExecOnCompletion exec) {
        log.log(Level.FINE, "CouchbaseManager.doSessionDelete(Session,ExecOnCompletion): init {0} {1}", 
                new Object[]{session.toString(), exec});
        session.waitOnExecution();
        ClientRequest req;
        if (this.isSticky()) {
            req = client.delete(session);
        } else {
            req = client.delete(session, session.getCas());
        }
        if (exec == null) {
            ClientResult res = client.waitForCompletion(req, this.operationTimeout);
            if (!res.isSuccess()) {
                session.setMemStatus(SessionMemStatus.ERROR);
                IllegalStateException e = new IllegalStateException(res.getStatus().getMessage(), res.getException());
                log.log(Level.SEVERE, "Error deleting in the repo. ", e);
                throw e;
            } else {
                session.setCas(-1);
                session.setMemStatus(SessionMemStatus.NOT_EXISTS);
            }
            req = null;
        } else {
            client.execOnCompletion(req, this.operationTimeout, exec);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionDelete(Session,ExecOnCompletion): exit {0}", req);
        return req;
    }
    
    /**
     * Performs a unlock of the session. This method is used when session was
     * not modified and it just needs to be released.
     * @param session The session to unlock
     * @param exec If not null the method is executed asynchronously
     * @return the request executing if exec is not null or null is not async
     */
    public ClientRequest doSessionUnlock(CouchbaseWrapperSession session, ExecOnCompletion exec) {
        log.log(Level.FINE, "CouchbaseManager.doSessionUnlock(Session,ExecOnCompletion): init {0} {1}", 
                new Object[]{session.toString(), exec});
        session.waitOnExecution();
        ClientRequest req = client.unlock(session, session.getCas());
        if (exec == null) {
            ClientResult res = client.waitForCompletion(req, this.operationTimeout);
            if (!res.isSuccess()) {
                session.setMemStatus(SessionMemStatus.ERROR);
                IllegalStateException e = new IllegalStateException(res.getStatus().getMessage(), res.getException());
                log.log(Level.SEVERE, "Error unlocking in the repo.", e);
                throw e;
            }
            req = null;
        } else {
            client.execOnCompletion(req, this.operationTimeout, exec);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionUnlock(Session,ExecOnCompletion): exit {0}", req);
        return req;
    }
    
    /**
     * Performs a touch (expiration is refreshed) over a session. This method is
     * used when session was accessed but not modified (there is not a 
     * touchAndUnlock or unlockAndTouch method).
     * @param session The session to touch
     * @param exec If not null the method is executed asynchronously
     * @return the request executing if exec is not null or null is not async
     */
    public ClientRequest doSessionTouch(CouchbaseWrapperSession session, ExecOnCompletion exec) {
        log.log(Level.FINE, "CouchbaseManager.doSessionTouch(Session,ExecOnCompletion): init {0} {1}", 
                new Object[]{session.toString(), exec});
        session.waitOnExecution();
        ClientRequest req = client.touch(session, this.getMaxInactiveInterval());
        if (exec == null) {
            ClientResult res = client.waitForCompletion(req, this.operationTimeout);
            if (!res.isSuccess()) {
                session.setMemStatus(SessionMemStatus.ERROR);
                IllegalStateException e = new IllegalStateException(res.getStatus().getMessage(), res.getException());
                log.log(Level.SEVERE, "Error touching in the repo.", e);
                throw e;
            }
            req = null;
        } else {
            client.execOnCompletion(req, this.operationTimeout, exec);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionTouch(Session,ExecOnCompletion): exit {0}", req);
        return req;
    }
    
    /**
     * Adds a new session to the couchbase server when a new session is created.
     * @param session The new session to be added.
     */
    public void doSessionAdd(CouchbaseWrapperSession session) {
        log.log(Level.FINE, "CouchbaseManager.doSessionAdd(Session): init {0}", session.toString());
        session.waitOnExecution();
        try {
            ClientRequest req = client.add(session, this.getMaxInactiveInterval());
            ClientResult res = client.waitForCompletion(req, this.operationTimeout);
            if (!res.isSuccess()) {
                session.setMemStatus(SessionMemStatus.ERROR);
                IllegalStateException e = new IllegalStateException(res.getStatus().getMessage(), res.getException());
                log.log(Level.SEVERE, "Error adding in the repo.", e);
                throw e;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error adding the session in the repo", e);
        }
        log.fine("CouchbaseManager.doSessionAdd(Session): exit");
    }
    
    /**
     * Invalidate all sessions that have expired. The main idea is that
     * all sessions are iterated and, one by one, local expiration is
     * checked. If it is locally expired the full process is performed (locked,
     * check internally, and unlocked or expired).
     */
    @Override
    public void processExpires() {
        log.fine("CouchbaseManager.processExpires(): init");
        long timeNow = System.currentTimeMillis();
        Session[] current = findSessions();
        if (current != null) {
            for (Session session : current) {
                CouchbaseWrapperSession sess = (CouchbaseWrapperSession) session;
                if (sess.localHasExpired()) {
                    // only block if it is expired locally, this
                    // way avoid access repo until is accessed locally
                    if (sess.lockBackground()) {
                        try {
                            sess.isValid();
                        } finally {
                            sess.unlockBackground();
                        }
                    }
                }
            }
        }
        long timeEnd = System.currentTimeMillis();
        log.log(Level.FINE, "CouchbaseManager.processExpires(): exit. {0} sessions processed in {1} ms", 
                new Object[] {current.length, (timeEnd - timeNow)});
    }
    
}
