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

import com.sun.web.security.RealmAdapter;
import es.rickyepoderi.couchbasemanager.couchbase.ClientData;
import es.rickyepoderi.couchbasemanager.couchbase.ClientRequest;
import es.rickyepoderi.couchbasemanager.couchbase.ClientResult;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

/**
 *
 * <p>The CouchbaseWrapperSession is a extension of the StandardSession but with
 * to main differences in behaviour: session is only expired when expired in
 * the external repo (session does not exist in couchbase server) and session
 * contents are only valid when locked if non-sticky (locking is also done in couchbase). 
 * So two ideas are the bases of the class:</p>
 * 
 * <ul>
 * <li>lockForeground and lockBackground when non-sticky methods
 * perform a real locking in couchbase (no other server will modify the
 * session). When the session is released (unlock methods) session is saved
 * in the repository. In sticky no lock/unlock is done in couchbase and session
 * is not read again and only saved at unlock.</li>
 * <li>Expiration is managed locally (to save external calls) but real 
 * expiration occurs when the session disappears from the repository.
 * Expiration is also managed by couchbase.</li>
 * </ul>
 * 
 * <p>So the differences between CouchbaseWrapperSession and StandardSession rely in
 * this class trust only in the couchbase server. The session adds mainly two
 * status to the standard session used in glassfish or tomcat:</p>
 * 
 * <ul>
 * <li>The session status of the memory repository:
 *   <ul>
 *   <li>OK: Session is load and locked (only when locked the session
 *       is completely sure of not being modified by another server).</li>
 *   <li>NOT_LOADED: Session is empty or cleared, in this state the session
 *       is not locked and data can be modified by another server. In this
 *       state attributes are empty (session is lighter).</li>
 *   <li>NOT_EXISTS: Session was tried to be read from the server but it
 *       does not exist (expired or deleted by another server). this
 *       situation must be true to be expired.</li>
 *   <li>FOREGROUND_LOCK: Blocked by lockForeground method, in non-sticky
 *       the session is blocked in couchbase.</li>
 *   <li>BACKGROUND_LOCK: Blocked by lockBackground method, in non-sticky
 *       the session is blocked in couchbase</li>
 *   <li>ALREADY_LOCKED: The session was tried to be locked (couchnase) but
 *       it was already locked by another server.</li>
 *   <li>ERROR: Some error loading the session (unknown state). This state
 *       is used to mark a re-read from couchbase, it is used when errors
 *       founds (background save/touch produces an error to be omitted).</li>
 *   </ul>
 * </li>
 * </ul>
 * 
 * 
 * @author ricky
 */
public class CouchbaseWrapperSession extends StandardSession 
  implements ClientData {
    
    /**
     * Enum class that represents all the status the session can have
     * relatively to the couchbase server: OK (session ok and full read), 
     * NOT_LOADED (session is cleared or not already loaded), NOT_EXISTS 
     * (session was read from the repo but it does not exist), BLOCKED 
     * (session was blocked at reading) and ERROR (error reading the session 
     * in the repo).
     */
    protected static enum SessionMemStatus {
        NOT_LOADED,
        FOREGROUND_LOCK,
        BACKGROUND_LOCK,
        NOT_EXISTS,
        ALREADY_LOCKED,
        ERROR;
        
        /**
         * Return if the mem status id a locked one (foreground or background)
         * @return true if the status is one of the locked ones
         */
        public boolean isLocked() {
            return SessionMemStatus.BACKGROUND_LOCK.equals(this)
                    || SessionMemStatus.FOREGROUND_LOCK.equals(this);
        }
        
        /**
         * When a session is loaded the success status are NOT_LOADED (session
         * was read but with no lock) or locked (both lock status).
         * @return true if session is read ok (means the three commented states)
         */
        public boolean isSuccess() {
            return SessionMemStatus.NOT_LOADED.equals(this) || isLocked();
        }
    };
    
    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(CouchbaseWrapperSession.class.getName());
    
    /**
     * Glassfish declared the principal as transient, so the principal
     * is lost when serializing/de-serializing the session. Store the username
     * here as it does in 
     * <a href="http://java.net/projects/glassfish/sources/svn/content/trunk/main/appserver/web/web-ha/src/main/java/org/glassfish/web/ha/session/management/ReplicationAttributeStore.java">ReplicationAttributeStore</a>
     * 
     */
    protected String username = null;
    
    /**
     * CAS for the session (normal CAS read from couchbase or special negative meanings).
     */
    protected transient long cas = -1;
    
    /**
     * Status of the session against the couchbase external repository.
     */
    protected transient SessionMemStatus mstatus = SessionMemStatus.NOT_LOADED;
    
    /**
     * Number of times the session has been foreground locked.
     */
    protected transient int numForegroundLocks = 0;
    
    /**
     * Request that is being processed. The request that is processed in the
     * background is stored here to wait in case another operation comes.
     */
    protected transient ClientRequest req = null;
    
    //
    // CONSTRUCTORS
    //
    
    /**
     * Constructor of the session. Initialy the session is NOT_LOADED,
     * dirty and accessed (obviously not locked).
     * 
     * @param manager The mem manager
     */
    public CouchbaseWrapperSession(Manager manager) {
        super(manager);
        this.cas = -1;
        this.mstatus = SessionMemStatus.NOT_LOADED;
        this.numForegroundLocks = 0;
        log.log(Level.FINE, "CouchbaseWrapperSession.constructor(Manager): init {0}", manager);
    }
    
    /**
     * Fake constructor. It is used cos when finding a session in couchbase a
     * session should be provided. This method is a fake and shouldn't be used
     * when a real session is created. This method DO NOT add the session
     * to the session map in the manager. That is the reason to be protected.
     * @param manager The manager of the session
     * @param id The id of the session to look for in couchbase
     */
    protected CouchbaseWrapperSession(Manager manager, String id) {
        this(manager);
        // not using setId cos it adds the session to the manager
        this.id = id;
        // force re-read
        this.mstatus = SessionMemStatus.ERROR;
    }
    
    //
    // GETTERS AND SETTERS
    //
    
    /**
     * Return the CAS for the session.
     * @return The CAS
     */
    public long getCas() {
        return this.cas;
    }
    
    /**
     * Set a new CAS for the session.
     * @param cas The new CAS
     */
    public void setCas(long cas) {
        this.cas = cas;
    }

    /**
     * Return the session status of the session.
     * @return The session status
     */
    public SessionMemStatus getMemStatus() {
        return mstatus;
    }

    /**
     * Assign new session status.
     * @param status New session status
     */
    public void setMemStatus(SessionMemStatus status) {
        this.mstatus = status;
    }
    
    public ClientRequest getClientRequest() {
        return this.req;
    }
    
    /**
     * Clears the request and the attributes if session is non-sticky. If the
     * operation was an error the session is marked to ERROR and the next
     * time session is re-read (no matter sticky or non-sticky). This method
     * is called after an async save, touch or delete.
     * Finally the notifyAll of the session is called to continue if some
     * other thread was waiting.
     * @param res The result of the operation in the background
     */
    synchronized public void clearRequestAndNotify(ClientResult res) {
        log.log(Level.FINE, "CouchbaseWrapperSession.clearRequest(): init/exit {0}", res);
        // clear attributes if non-sticky
        if (!((CouchbaseManager)manager).isSticky()) {
            this.attributes.clear();
        }
        // set to error if some error has ocurred
        if (!res.isSuccess()) {
            log.log(Level.SEVERE, "Error in the background operation. Marking the session to ERROR", 
                    new IllegalStateException(res.getStatus().getMessage(), res.getException()));
            setMemStatus(SessionMemStatus.ERROR);
        }
        this.req = null;
        this.notifyAll();
    }
    
    //
    // METHODS TO ACCESS THE COUCHBASE SERVER
    // Methods that should always be synchronized. Perform real interaction
    // with couchbase server but always calling the MemManager.
    //
    
    /**
     * Clear the session. That means the transient vars are cleared to the
     * next request-
     */
    synchronized protected void clear() {
        this.cas = -1;
        setMemStatus(SessionMemStatus.NOT_LOADED);
    }
    
    /**
     * Fill the session with a couchbase read session. Attributes are
     * refilled, access time and last access time are refreshed. CAS is
     * retrieved from the couchbase session. If the session is not locked
     * only access data is refreshed (access time and so on).
     * 
     * @param loaded The loaded session from couchbase
     * @param status The status of the session
     * @param cas The cas read
     */
    synchronized protected void fill(CouchbaseWrapperSession loaded, SessionMemStatus status, long cas) {
        // set the fixed parts
        this.setSipApplicationSessionId(loaded.getSipApplicationSessionId());
        this.setBeKey(loaded.getBeKey());
        this.creationTime = loaded.creationTime;
        this.maxInactiveInterval = loaded.maxInactiveInterval;
        this.isNew = loaded.isNew;
        this.isValid = loaded.isValid;
        this.version = loaded.version;
        this.ssoId = loaded.ssoId;
        if (loaded.username != null && 
                (this.principal == null || !loaded.username.equals(this.principal.getName()))){
            // add the principal if the loaded session has one and 
            // current one is empty or different
            log.log(Level.FINE, "CouchbaseWrapperSession.fill(): username={0}", loaded.username);
            Principal p = ((RealmAdapter) this.manager.getContainer().getRealm())
                    .createFailOveredPrincipal(loaded.username);
            this.setPrincipal(p);
        }
        // set accessed time only if greater
        if (loaded.thisAccessedTime > this.thisAccessedTime) {
            this.thisAccessedTime = loaded.thisAccessedTime;
        }
        if (loaded.lastAccessedTime > this.lastAccessedTime) {
            this.lastAccessedTime = loaded.lastAccessedTime;
        }
        // attributes are loaded only if sticky or non-sticky but locked
        if (status.isLocked() || ((CouchbaseManager) manager).isSticky()) {
            this.attributes = loaded.attributes;
        }
        // set new mstatus and cas
        setMemStatus(status);
        this.cas = cas;
    }
    
    /**
     * Save this session into couchbase. Only if status was OK the method
     * executes a real save. If dirty CAS save method is used, but if the
     * session was only accessed a touch is done. If not modified and not 
     * accessed only unlocking is performed. There is a special case when the
     * session is only accessed but in the repo local timestamps are too old
     * (the session has been accessed for a long time).
     * TODO: if not dirty but accessed two methods are executed (touch and
     * unlock) cos couchbase client has not touchAndUnlock.
     */
    synchronized protected void doSave() {
        if (this.isLocked()) {
            log.log(Level.FINE, "CouchbaseWrapperSession.doSave(): init");
            // session saved if modified or long time not accessed
            req = ((CouchbaseManager) manager).doSessionSave(this,
                    new OperationComplete(this));

        }
        // clear transient vars
        this.clear();
    }
    
    /**
     * Reaload the session from the couchbase repo. The lock parameter 
     * establish if it is a normal refresh or a locked one. The 
     * session is read from the repo and filled. CAS is always set (cas or
     * status is modified accordingly).
     * 
     * @param expected The expected status (both locks or not load)
     */
    synchronized protected void doLoad(SessionMemStatus expected) {
        ((CouchbaseManager) manager).doSessionLoad(this, expected);
    }
    
    /**
     * Method that waits for a previous request to complete. Cos now save/touch
     * methods are asynchronously executed when accessing again to couchbase
     * a previous operation can be still running. This methods waits patiently
     * the operation to finish.
     */
    synchronized protected void waitOnExecution() {
        while (req != null) {
            try {
                log.fine("CouchbaseManager.waitOnExecution(): waiting execution");
                // wait for the execution to finish
                this.wait();
            } catch (Exception e) {
                // here it doesn't matter if some exception happens 
                // (Interrupted, NullPointer or whatever) cos the req is
                // rechecked til finish or null.
            }
        }
    }
    
    //
    // LOCKING METHODS
    //

    /**
     * Check if the session has any kind of locking.
     * @return true if a foreground or background lock exists.
     */
    synchronized public boolean isLocked() {
        return this.mstatus.isLocked();
    }
    
    /**
     * Check if the session is foreground locked.
     * @return true if the session has a lock and it is foreground
     */
    @Override
    synchronized public boolean isForegroundLocked() {
        return SessionMemStatus.FOREGROUND_LOCK.equals(this.mstatus);
    }
    
    /**
     * Check if the session is background locked.
     * @return true if the session is locked and the lock is background.
     */
    synchronized public boolean isBackgroundLocked() {
        return SessionMemStatus.BACKGROUND_LOCK.equals(this.mstatus);
    }    
    
    /**
     * Performs a lock background over the session. The main methods of the 
     * couchbase session behavior, in the locking the session is load from
     * the repo and locked (no other app server will modify it during the
     * lock). The session contents are assured while blocked.
     * @return true if the session was correctly locked in couchbase.
     */
    @Override
    synchronized public boolean lockBackground() {
        log.fine("CouchbaseWrapperSession.lockBackground(): init");
        if (isForegroundLocked()) {
            // the session is foreground locked => false
            return false;
        }
        if (!isLocked()) {
            // session is first locked => load with background lock
            doLoad(SessionMemStatus.BACKGROUND_LOCK);
            if (SessionMemStatus.ALREADY_LOCKED.equals(this.mstatus)
                    || SessionMemStatus.ERROR.equals(this.mstatus)) {
                // if blocked or error at reading try again
                // if not found or ok blocking can be done
                // not found means the session is expired but 
                // it can be blocked anyways
                return false;
            }
        }
        // the sesison id loaded and locked => mark and return
        this.numForegroundLocks = 0;
        log.fine("CouchbaseWrapperSession.lockBackground(): exit");
        return true;
    }

    /**
     * Performs a lock foreground over the session. The main methods of the 
     * couchbase session behavior, in the locking the session is load from
     * the repo and locked (no other app server will modify it during the
     * lock). The session contents are assured while blocked.
     * @return true if the session was correctly locked in couchbase.
     */
    @Override
    synchronized public boolean lockForeground() {
        log.fine("CouchbaseWrapperSession.lockForeground(): init");
        if (isBackgroundLocked()) {
            // is background locked => return false
            return false;
        }
        if (!isLocked()) {
            // session is first locked => load with lock
            doLoad(SessionMemStatus.FOREGROUND_LOCK);
            if (SessionMemStatus.ALREADY_LOCKED.equals(this.mstatus)
                    || SessionMemStatus.ERROR.equals(this.mstatus)) {
                // if blocked or error at reading try again
                // if not found or ok blocking can be done
                // not found means the session is expired but 
                // it can be blocked anyways
                return false;
            }
        }
        // the sesison id loaded and locked => mark and return
        this.numForegroundLocks++;
        log.log(Level.FINE, "CouchbaseWrapperSession.lockForeground(): exit {0}", numForegroundLocks);
        return true;
    }

    /**
     * Lock is released in couchbase. The lock is released and the session 
     * contents saved in the repo (the session can be saved completely or
     * just touched, it depends in access status).
     */
    @Override
    synchronized public void unlockBackground() {
        log.fine("CouchbaseWrapperSession.unlockBackground(): init");
        if (!isLocked()) {
            // not locked => just return
            return;
        }
        if (isBackgroundLocked()) {
            // save the session
            doSave();
            // free the lock
            this.numForegroundLocks = 0;
        }
        log.fine("CouchbaseWrapperSession.unlockBackground(): exit");
    }

    /**
     * Lock is released in couchbase. The lock is released and the session 
     * contents saved in the repo (the session can be saved completely or
     * just touched, it depends in access status).
     */
    @Override
    synchronized public void unlockForeground() {
        log.fine("CouchbaseWrapperSession.unlockForeground(): init");
        //in this case we are not using locks so just return true
        if (!isLocked()) {
            // not locked => just return
            return;
        }
        if (isForegroundLocked()) {
            // decrement lock number
            this.numForegroundLocks--;
            if (this.numForegroundLocks == 0) {
                // save the session
                doSave();
                // free the lock
            }
        }
        log.log(Level.FINE, "CouchbaseWrapperSession.unlockForeground(): exit {0}", numForegroundLocks);
    }

    /**
     * Lock is released in couchbase. The lock is released and the session 
     * contents saved in the repo (the session can be saved completely or
     * just touched, it depends in access status).
     */
    @Override
    synchronized public void unlockForegroundCompletely() {
        log.fine("CouchbaseWrapperSession.unlockForegroundCompletely(): init");
        // free any possible lock
        this.numForegroundLocks = 0;
        if (!isLocked()) {
            // not locked => just return
            return;
        }
        if (isForegroundLocked()) {
            // save the session
            doSave();
        }
        log.fine("CouchbaseWrapperSession.unlockForegroundCompletely(): exit");
    }
    
    //
    // EXPIRATION METHODS
    //
    
    /**
     * Expire the session normally. Besides the session is marked as NOT_EXISTS.
     */
    @Override
    public void expire() {
        super.expire();
        synchronized (this) {
            // mark the session has been deleted from the repo
            this.cas = -1;
            setMemStatus(SessionMemStatus.NOT_EXISTS);
        }
    }
    
    /**
     * The local expired check uses the normal standard session method. So the
     * session checks access timestamps in the session, this method never
     * is trusted when return true (real check must be executed). The idea
     * is only check sessions already locally expired. The method is marked as
     * public to save couchbase calls.
     * 
     * @return true if the local timestamps mark the session as expired
     */
    public boolean localHasExpired() {
        return super.hasExpired();
    }
    
    /**
     * Real hasExpired method. The method first tries to answer using real
     * status (only is real for sure it session is locked => not expired or
     * it is NOT_EXISTS => expired). If the status does not gives a definitive
     * clue the local timestamps (call localHasExpired method) and, if expired, 
     * load the session from the  couchbase repository, to be sure that the 
     * session is expired. Obviously there is a chance of saying not expired 
     * when expired (but as soon as the session is locked, real answer
     * is given). This is done to save requests to the couchbase server.
     * 
     * @return true if the session does not exist in the couchbase repository.
     */
    @Override
    synchronized public boolean hasExpired() {
        log.fine("CouchbaseWrapperSession.hasExpired(): init");
        boolean expired;
        if (isLocked() && !((CouchbaseManager) manager).isSticky()) {
            // session is currently locked in non-sticky => not expired
            expired = false;
        } else if (SessionMemStatus.NOT_EXISTS.equals(this.mstatus)) {
            // session is NOT_EXISTS => expired
            expired = true;
        } else {
            // session is in other state => expired not known for sure
            // check lock expiration and if expired re-check with a load.
            // Take in mind that the session can be deleted by other server
            // but we are saying it is alive (as soon as the session is locked
            // real value takes precedence)
            expired = localHasExpired();
            if (expired) {
                doLoad(SessionMemStatus.NOT_LOADED);
                // session is now refreshed => NOT_EXISTS only expired value
                expired = SessionMemStatus.NOT_EXISTS.equals(mstatus);
            }
        }
        log.log(Level.FINE, "CouchbaseWrapperSession.hasExpired(): exit {0}", expired);
        return expired;
    }

    //
    // METHOD THAT MANAGES ACCESS STATUS
    //
    
    
    /**
     * Access the session. The session is also marked as ACCESSED if it was
     * clean.
     */
    @Override
    public void access() {
        super.access();
    }
    
    /**
     * Remove attribute. Normal method but marking state as dirty.
     * @param name The name of the attribute to remove
     */
    @Override
    public void removeAttribute(String name) {
        super.removeAttribute(name);
    }

    /**
     * Set attribute. Normal method but marking state as dirty.
     * @param name The name of the attribute to set
     * @param value The new value to set
     */
    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
    }

    /**
     * Remove Attribute. Normal method but marking state as dirty.
     * @param name The name of the attribute to remove
     * @param notify Should we notify interested listeners that this attribute 
     *        is being removed?
     * @param checkValid Indicates whether IllegalStateException must be thrown 
     *        if session has already been invalidated
     */
    @Override
    public void removeAttribute(String name, boolean notify, boolean checkValid) {
        super.removeAttribute(name, notify, checkValid);
    }

    /**
     * Return the attribute associated with this name. The session is marked
     * as dirty if the attribute is not a simple object (String, Boolean or
     * Number).
     * @param name The name of the attribute top return
     * @return The attribute value
     */
    @Override
    public Object getAttribute(String name) {
        Object attr = super.getAttribute(name);
        return attr;
    }

    /**
     * Setter for the principal. Set the principal as usual but the 
     * principal name is stored also 
     * @param principal 
     */
    @Override
    public void setPrincipal(Principal principal) {
        super.setPrincipal(principal);
        // take note of the principal to be saved in couchbase
        if (principal != null) {
            this.username = principal.getName();
        }
    }
    
    
    /**
     * Debug method.
     * @return string representation of the session
     */
    @Override
    public String toString() {
        if (log.isLoggable(Level.FINE)) {
            return new StringBuffer(this.getClass().getSimpleName())
                .append(" ")
                .append(this.mstatus)
                .append(" [")
                .append(this.id)
                .append("] hash:")
                .append(Integer.toHexString(hashCode()))
                .append(" cas:")
                .append(cas)
                .toString();
        } else {
            return this.id;
        }
    }
    
}