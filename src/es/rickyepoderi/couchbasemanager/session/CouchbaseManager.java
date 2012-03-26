/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.session;


import com.couchbase.client.CouchbaseClient;
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
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.internal.OperationFuture;
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
 * supposed that any modification is always done in locked session state).</li>
 * <li>Sessions are also stored in normal Map of StandardManager but
 * the session is cleared when not used (cleared means attributes are
 * removed).</li>
 * <li>The majority of this process lays inside MemWrapperSession implementation.
 * This special session clears its content inside unlock methods and loads it
 * from the memory repository in lock ones.</li>
 * <li>MemWrapperSession uses a activity status flag to record any attribute 
 * modification. If the session is dirty it is saved in the external repository, 
 * if accessed just touched. Obviously session is always unlocked.</li>
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
 * </ul>
 * 
 * <p>Restrictions in this first implementation:</p>
 *
 * <ul>
 * <li>Only one couchbase client is used for all the sessions. 
 * <a href="http://stackoverflow.com/questions/8683142/memcached-client-opening-closing-and-reusing-connections">
 * Reading information about spymemcached</a> it seems that it supports multi-thread
 * access and there is no need of pooling. For the moment a single connection
 * is used.</li>
 * <li>The single connection seems to support reconnection. Not checked.</li>
 * <li>Maybe better use async methods to improve performance (but some
 * checking will be needed).</li>
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
     * Variable that indicates if the manager is started or not
     */
    private boolean started = false;
    
    /**
     * Name of the manager
     */
    protected static final String name = "CouchbaseManager";
    
    /**
     * The descriptive information about this implementation.
     */
    private static final String info = "CouchbaseManager / 0.1";
    
    /**
     * spymemcached client to communicate with the memory repository
     */
    protected CouchbaseClient client = null;
    
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
        log.log(Level.FINE, "CouchbaseManager.add: init {0}", session);
        super.add(session);
        log.fine("CouchbaseManager.add: exit");
    }

    /**
     * Not implemented now.
     * @param session 
     */
    @Override
    public void changeSessionId(Session session) {
        //log.log(Level.FINE, "CouchbaseManager.changeSessionId: init {0}", session);
        //String oldId = session.getId();
        //  change session id
        //super.changeSessionId(session);
        // delete old session in the memory store
        //doSessionDelete(oldId);
        //doSessionSave(session);
        throw new UnsupportedOperationException("changeSessionId not implemented!");
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
        Session session = createSessionInternal(id);
        log.log(Level.FINE, "CouchbaseManager.createSession(): exit {0}", session);
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
        log.log(Level.FINE, "CouchbaseManager.createSession: init {0}", id);
        Session session = createSessionInternal(id);
        log.log(Level.FINE, "CouchbaseManager.createSession(String): exit {0}", session);
        return session;
    }
    
    /**
     * Internal method that creates a new session initialized. The session
     * is initialized with some values (timestamps and so on).
     * @param id The session identifier.
     * @return The new session.
     */
    public Session createSessionInternal(String id) {
        log.log(Level.FINE, "CouchbaseManager.createSessionInternal: init {0}", id);
        // Recycle or create a Session instance
        CouchbaseWrapperSession session = (CouchbaseWrapperSession) createEmptySession();
        // Initialize the properties of the new session and return it
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(this.maxInactiveInterval);
        session.setId(id);
        // add session to both internal and external
        doSessionAdd(session);
        session.lockForeground();
        add(session);
        sessionCounter++;
        log.log(Level.FINE, "CouchbaseManager.createSessionInternal(String): exit {0}", session);
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
        log.log(Level.FINE, "CouchbaseManager.findSession: init {0}", id);
        CouchbaseWrapperSession session = (CouchbaseWrapperSession) super.findSession(id);
        if (session == null) {
            // search for the session in repository
            SessionLoadResult result = doSessionLoad(id, SessionMemStatus.NOT_LOADED);
            if (result.getMemStatus().isSuccess()) {
                // add the session to current sessions in this manager but
                // it is NOT_LOADED til locking (fill is called to initialize all params)
                session = result.getSession();
                session.setManager(this);
                session.fill(result);
                add(session);
            } else if (SessionMemStatus.ALREADY_LOCKED.equals(result.getMemStatus())) {
                // the session exists bit it is blocked 
                // just create it empty and wait to lock to get fully read
                session = (CouchbaseWrapperSession) createEmptySession();
                session.setId(id);
            }
        }
        log.log(Level.FINE, "CouchbaseManager.findSession(String): exit {0}", session);
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

    /**
     * Method that is called from StandardSession inside expire method.
     * The session is removed from the internal map and from the external 
     * repository (if not already in NOT_EXISTS state).
     * @param session The session to remove
     */
    @Override
    public void remove(Session session) {
        log.log(Level.FINE, "CouchbaseManager.remove: init {0}", session);
        super.remove(session);
        if (!SessionMemStatus.NOT_EXISTS.equals(((CouchbaseWrapperSession) session).getMemStatus())) {
            doSessionDelete((CouchbaseWrapperSession) session);
        }
        log.fine("CouchbaseManager.remove: exit");
    }

    /**
     * Expires a session, just calling StandardManager super method.
     * 
     * @param id The session id to expire.
     */
    @Override
    public void expireSession(String id) {
        log.log(Level.FINE, "CouchbaseManager.expireSession: init {0}", id);
        super.expireSession(id);
        log.fine("CouchbaseManager.expireSession: exit");
    }
    
    //
    // Methods that I don't know if they have to been implemented
    //

    @Override
    public void update(HttpSession session) throws Exception {
        log.log(Level.FINE, "CouchbaseManager.update: init/exit {0}", session);
    }

    @Override
    public void preRequestDispatcherProcess(ServletRequest request, ServletResponse response) {
        log.log(Level.FINE, "CouchbaseManager.preRequestDispatcherProcess: init/exit {0} {1}", 
                new Object[] {request, response});
    }

    @Override
    public void postRequestDispatcherProcess(ServletRequest request, ServletResponse response) {
        log.log(Level.FINE, "CouchbaseManager.postRequestDispatcherProcess: init/exit {0} {1}",
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
            // TODO: add parameters to URLs, bucketname, username and password
            // start the memcached client
            ArrayList baseURIs = new ArrayList();
            String[] uris = this.url.split(",");
            for (String uri: uris) {
                uri = uri.trim();
                baseURIs.add(new URI(uri));
            }
            //client = new CouchbaseClient(baseURIs, "default", null, "");
            client = new CouchbaseClient(baseURIs, this.bucket, this.username, this.password);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error initiliazing spymemcached client...", e);
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
     * Start the manager. Initializes the manager and start.
     * @throws LifecycleException Some error.
     */
    @Override
    public void start() throws LifecycleException {
        log.fine("CouchbaseManager.start: init");
        if (!initialized) {
            init();
        }
        // Validate and update our current component state
        if (started) {
            log.info("CouchbaseManager already alreadyStarted");
            return;
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;
        // Force initialization of the random number generator
        log.finest("Force random number initialization starting");
        generateSessionId();
        log.fine("CouchbaseManager.start: exit");
    }

    /**
     * Stop the manager.
     * @throws LifecycleException  Some error.
     */
    @Override
    public void stop() throws LifecycleException {
        log.fine("CouchbaseManager.stop: init");
        if (!started) {
            throw new LifecycleException("CouchbaseManager not started");
        }
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;
        // stop the random engine
        this.random = null;
        // destroy manager
        if (initialized) {
            destroy();
        }
        log.fine("CouchbaseManager.stop: exit");
    }
    
    //
    // Extra method to save session inside the valve
    //
    
    /**
     * Loads a new session from the couchbase repository. The session is 
     * read using gets or getAndLock (lock parameter is used to choose the 
     * method). The result is filled with the CAS returned or error if
     * some error is returned.
     * @param id The id of the session to load
     * @param expected The result that is wanted (NOT_LOADED, FOREGROUND_LOCK, BACKGROUND_LOCK)
     * @return The session result (cas, status and session)
     */
    public SessionLoadResult doSessionLoad(String id, SessionMemStatus expected) {
        log.log(Level.FINE, "CouchbaseManager.doSessionLoad(String,boolean): {0} {1}", 
                new Object[]{id, expected});
        SessionLoadResult result = new SessionLoadResult();
        CouchbaseWrapperSession session = null;
        try {
            OperationFuture<CASValue<Object>> future;
            if (expected.isLocked()) {
                future = client.asyncGetAndLock(id, 30); // TODO: add configurable timeout
            } else {
                future = client.asyncGets(id);
            }
            if (future.getStatus().isSuccess()) {
                log.fine("The session was in the repository, returning it");
                session = (CouchbaseWrapperSession) future.get().getValue();
                result.setCas(future.get().getCas());
                result.setSession(session);
                result.setMemStatus(expected);
            } else if (future.getStatus().getMessage().equals("NOT_FOUND")) {
                log.fine("NOT_FOUND => session doesn't exist in the repo");
                result.setCas(-1);
                result.setMemStatus(SessionMemStatus.NOT_EXISTS);
            } else if (future.getStatus().getMessage().equals("LOCK_ERROR")) {
                log.fine("LOCK_ERROR => session exists but not read");
                result.setCas(-1);
                result.setMemStatus(SessionMemStatus.ALREADY_LOCKED);
            } else {
                log.log(Level.SEVERE, "Error loading from the repo {0}", future.getStatus());
                result.setCas(-1);
                result.setMemStatus(SessionMemStatus.ERROR);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error loading from the repo", e);
            result.setCas(-1);
            result.setMemStatus(SessionMemStatus.ERROR);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionLoad(String): exit {0}", session);
        return result;
    }
    
    /**
     * Performs a session save using the CAS. The session is saved and touched
     * (expiration is refreshed), the method uses secure CAS method to prevent
     * multi-access errors.
     * TODO: cas has no exp method without transcoder.
     * @param session The session to save.
     */
    public void doSessionSave(CouchbaseWrapperSession session) {
        log.log(Level.FINE, "CouchbaseManager.doSessionSave(MemWrapperSession): init {0}", session);
        // save the session inside reposiroty
        try {
            CASResponse res = client.cas(session.getId(), session.getCas(),
                    this.getMaxInactiveInterval(), session, 
                    new DefaultConnectionFactory().getDefaultTranscoder()); // TODO: set a transcoder via configuration
            if (!CASResponse.OK.equals(res)) {
                log.log(Level.SEVERE, "Error saving in the repo. Return is not OK {0}", res);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error saving in the repo", e);
        }
        log.fine("CouchbaseManager.doSessionSave(MemWrapperSession): exit");
    }
    
    /**
     * Deletes a session from the couchbase server.
     * @param session The session to delete.
     * @return Result of the delete operation
     */
    public boolean doSessionDelete(CouchbaseWrapperSession session) {
        log.log(Level.FINE, "CouchbaseManager.doSessionDelete(MemWrapperSession): init {0}", session);
        boolean deleted = false;
        try {
            if (session.isLocked()) {
                // TODO: cannot be deleted if locked (why????)
                doSessionUnlock(session.getId(), session.getCas());
            }
            OperationFuture<Boolean> res = client.delete(session.getId());
            deleted = res.get();
            if (!deleted) {
                log.log(Level.SEVERE, "Error deleting in the repo. Return is false");
            } else {
                session.setCas(-1);
                session.setMemStatus(SessionMemStatus.NOT_EXISTS);
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error deleting the session in the repo", e);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionDelete(MemWrapperSession): exit {0}", deleted);
        return deleted;
    }
    
    /**
     * Performs a unlock of the session. This method is used when session was
     * not modified and it just needs to be released.
     * @param id The session id
     * @param cas The CAS used in the locking.
     * @return true if ok.
     */
    public boolean doSessionUnlock(String id, long cas) {
        log.log(Level.FINE, "CouchbaseManager.doSessionUnlock(String, cas): init {0} {1}", 
                new Object[]{id, cas});
        boolean unlocked = client.unlock(id, cas);
        log.log(Level.FINE, "CouchbaseManager.doSessionUnlock(String, cas): exit {0}", unlocked);
        return unlocked;
    }
    
    /**
     * Performs a touch (expiration is refreshed) over a session. This method is
     * used when session was accessed but not modified (there is not a 
     * touchAndUnlock or unlockAndTouch method).
     * @param id The session id.
     * @return Result of the touch operation
     */
    public boolean doSessionTouch(String id) {
        log.log(Level.FINE, "CouchbaseManager.doSessionTouch(String): init {0}", id);
        boolean touch = false;
        try {
            OperationFuture<Boolean> res = client.touch(id, this.getMaxInactiveInterval());
            touch = res.get();
            if (!touch) {
                log.log(Level.SEVERE, "Error touching in the repo. Return is false");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error touching the session in the repo", e);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionTouch(String): exit {0}", touch);
        return touch;
    }
    
    /**
     * Adds a new session to the couchbase server when a new session is created.
     * @param session The new session to be added.
     * @return Result of the add operation
     */
    public boolean doSessionAdd(CouchbaseWrapperSession session) {
        log.log(Level.FINE, "CouchbaseManager.doSessionAdd(MemWrapperSession): init {0}", session);
        boolean added = false;
        try {
            OperationFuture<Boolean> res = client.add(session.getId(), 
                    this.getMaxInactiveInterval(), session);
            added = res.get();
            if (!added) {
                log.log(Level.SEVERE, "Error adding in the repo. Return is false");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error adding the session in the repo", e);
        }
        log.log(Level.FINE, "CouchbaseManager.doSessionAdd(MemWrapperSession): exit {0}", added);
        return added;
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
        log.log(Level.FINE, "CouchbaseManager.processExpires(): exit {0} ms", (timeEnd - timeNow));
    }
    
}
