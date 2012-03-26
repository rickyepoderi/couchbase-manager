/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.session;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

/**
 *
 * <p>The CouchbaseWrapperSession is a extension of the StandardSession but with
 * to main differences in behaviour: session is only expired when expired in
 * the external repo (session does not exist in couchbase server) and session
 * contents are only valid when locked (locking is also done in couchbase). 
 * So two ideas are the bases of the class:
 * 
 * <ul>
 * <li>lockForeground and lockBackGround methods
 * perform a real locking in couchbase (no other server will modify the
 * session). When the session is released (unlock methods) session is saved
 * in the repository (saved and unlocked if dirty, only touched and unlocked
 * if only accessed).</li>
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
 *   <li>BLOCKED: Session was tried to be read but it was blocked by
 *       another server. the lock* methods returns false and lock has to
 *       be tried again.</lI>
 *   <li>ERROR: Some error loading the session (unknown state).</li>
 *   </ul>
 * </li>
 * <li>The activity or accessed status. This status is the one check in order
 *     to save, touch and unlock the session when released.
 *   <ul>
 *   <li>CLEAN: The session is not modified.</li>
 *   <li>ACCESSED: The session is marked as accessed.</li>
 *   <li>DIRTY: The session is dirty (attributes have been modified).</lI>
 *   </ul>
 * </li>
 * </ul>
 * 
 * 
 * @author ricky
 */
public class CouchbaseWrapperSession extends StandardSession {
    
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
     * Enum class that marks the status of the session in the access point 
     * of view: CLEAN (not modified), ACCESSED (not modified but accessed),
     * DIRTY (accessed and modified).
     */
    protected static enum SessionAccessStatus {
        CLEAN, 
        ACCESSED, 
        DIRTY
    };
    
    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(CouchbaseWrapperSession.class.getName());
    
    /**
     * Maximum time to not save session if it is only accessed.
     */
    private static final long MAX_ACCESS_TIME_NOT_SAVING = 300000L;
    // TODO: Make this configurable????
    
    /**
     * CAS for the session (normal CAS read from couchbase or special negative meanings).
     */
    protected transient long cas = -1;
    
    /**
     * Status of the session against the couchbase external repository.
     */
    protected transient SessionMemStatus mstatus = SessionMemStatus.NOT_LOADED;
    
    /**
     * Status of the session in the access point of view.
     */
    protected transient SessionAccessStatus astatus = SessionAccessStatus.DIRTY;
    
    /**
     * Number of times the session has been foreground locked.
     */
    protected transient int numForegroundLocks = 0;
    
    /**
     * The access time stored in repo. This is used to save the session when
     * accessed if there is a big difference between both timestamps.
     */
    protected transient long repoAccessedTime = -1;
    
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
        this.astatus = SessionAccessStatus.DIRTY;
        this.numForegroundLocks = 0;
        log.log(Level.FINE, "CouchbaseWrapperSession.constructor(Manager): init {0}", manager);
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

    /**
     * Get the access or activity status.
     * @return The access status
     */
    public SessionAccessStatus getAstatus() {
        return astatus;
    }

    /**
     * Set a new access status
     * @param astatus The new status
     */
    public void setAstatus(SessionAccessStatus astatus) {
        this.astatus = astatus;
    }
    
    //
    // METHODS TO ACCESS THE COUCHBASE SERVER
    // Methods that should always be synchronized. Perform real interaction
    // with couchbase server but always calling the MemManager.
    //
    
    /**
     * Clear the session. That means the attributes are removed and status
     * marked as NOT_LOADED.
     */
    synchronized protected void clear() {
        this.attributes.clear();
        this.cas = -1;
        this.mstatus = SessionMemStatus.NOT_LOADED;
        this.astatus = SessionAccessStatus.CLEAN;
        this.repoAccessedTime = -1;
    }
    
    /**
     * Fill the session with a couchbase read session. Attributes are
     * refilled, access time and last access time are refreshed. CAS is
     * retieved from the couchbase session. If the session is not locked
     * only access data is refreshed (access time and so on).
     * 
     * @param result The result session that has been read from couchbase
     *               The result is assured OK
     */
    synchronized protected void fill(SessionLoadResult result) {
        CouchbaseWrapperSession loaded = result.getSession();
        if (loaded.thisAccessedTime > this.thisAccessedTime) {
            this.thisAccessedTime = loaded.thisAccessedTime;
        }
        if (loaded.lastAccessedTime > this.lastAccessedTime) {
            this.lastAccessedTime = loaded.lastAccessedTime;
        }
        if (result.getMemStatus().isLocked()) {
            this.attributes = loaded.attributes;
        }
        this.repoAccessedTime = loaded.thisAccessedTime;
        this.mstatus = result.getMemStatus();
        this.cas = result.getCas();
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
    synchronized protected final void doSave() {
        if (this.isLocked()) {
            if (SessionAccessStatus.DIRTY.equals(this.astatus) ||
                    (SessionAccessStatus.ACCESSED.equals(this.astatus) && 
                    (this.thisAccessedTime - this.repoAccessedTime > MAX_ACCESS_TIME_NOT_SAVING))) {
                // session saved if modified or long time not accessed
                ((CouchbaseManager) manager).doSessionSave(this);
                this.astatus = SessionAccessStatus.CLEAN;
            } else {
                if (SessionAccessStatus.ACCESSED.equals(this.astatus)) {
                    ((CouchbaseManager) manager).doSessionTouch(this.id);
                }
                ((CouchbaseManager) manager).doSessionUnlock(this.id, this.cas);
            }
        }
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
    synchronized protected final void doLoad(SessionMemStatus expected) {
        SessionLoadResult result = ((CouchbaseManager) manager).doSessionLoad(id, expected);
        if (result.getMemStatus().isSuccess()) {
            // load data and new cas
            fill(result);
        } else {
            // no session read => set state
            this.cas = -1;
            this.mstatus = result.getMemStatus();
        }
    }
    
    //
    // LOCKING METHODS
    //

    /**
     * Check if the session has any kind of locking.
     * @return true if a foreground or background lock exists.
     */
    public boolean isLocked() {
        return this.mstatus.isLocked();
    }
    
    /**
     * Check if the session is foreground locked.
     * @return true if the session has a lock and it is foreground
     */
    @Override
    public boolean isForegroundLocked() {
        return SessionMemStatus.FOREGROUND_LOCK.equals(this.mstatus);
    }
    
    /**
     * Check if the session is background locked.
     * @return true if the session is locked and the lock is background.
     */
    public boolean isBackgroundLocked() {
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
    public boolean lockBackground() {
        log.fine("CouchbaseWrapperSession.lockBackground(): init");
        synchronized (this) {
            if (isForegroundLocked()) {
                // the session is foreground locked => false
                return false;
            }
            if (!isLocked()) {
                // session is first locked => load with background lock
                doLoad(SessionMemStatus.BACKGROUND_LOCK);
                if (SessionMemStatus.ALREADY_LOCKED.equals(this.mstatus) ||
                    SessionMemStatus.ERROR.equals(this.mstatus)) {
                    // if blocked or error at reading try again
                    // if not found or ok blocking can be done
                    // not found means the session is expired but 
                    // it can be blocked anyways
                    return false;
                }
            }
            // the sesison id loaded and locked => mark and return
            //this.mstatus = SessionMemStatus.BACKGROUND_LOCK;
            this.numForegroundLocks = 0;
            return true;
        }
    }

    /**
     * Performs a lock foreground over the session. The main methods of the 
     * couchbase session behavior, in the locking the session is load from
     * the repo and locked (no other app server will modify it during the
     * lock). The session contents are assured while blocked.
     * @return true if the session was correctly locked in couchbase.
     */
    @Override
    public boolean lockForeground() {
        log.fine("CouchbaseWrapperSession.lockForeground(): init");
        synchronized (this) {
            if (isBackgroundLocked()) {
                // is background locked => return false
                return false;
            }
            if (!isLocked()) {
                // session is first locked => load with lock
                doLoad(SessionMemStatus.FOREGROUND_LOCK);
                if (SessionMemStatus.ALREADY_LOCKED.equals(this.mstatus) ||
                    SessionMemStatus.ERROR.equals(this.mstatus)) {
                    // if blocked or error at reading try again
                    // if not found or ok blocking can be done
                    // not found means the session is expired but 
                    // it can be blocked anyways
                    return false;
                }
            }
            // the sesison id loaded and locked => mark and return
            //this.mstatus = SessionMemStatus.FOREGROUND_LOCK;
            this.numForegroundLocks++;
            return true;
        }
    }

    /**
     * Lock is released in couchbase. The lock is released and the session 
     * contents saved in the repo (the session can be saved completely or
     * just touched, it depends in access status).
     */
    @Override
    public void unlockBackground() {
        log.fine("CouchbaseWrapperSession.unlockBackground(): init");
        synchronized (this) {
            if (!isLocked()) {
                // not locked => just return
                return;
            }
            if (isBackgroundLocked()) {
                // save the session
                doSave();
                // free the lock
                //this.mstatus = SessionMemStatus.NOT_LOADED;
                this.numForegroundLocks = 0;
            }
        }
    }

    /**
     * Lock is released in couchbase. The lock is released and the session 
     * contents saved in the repo (the session can be saved completely or
     * just touched, it depends in access status).
     */
    @Override
    public void unlockForeground() {
        log.fine("CouchbaseWrapperSession.unlockForeground(): init");
        //in this case we are not using locks so just return true
        synchronized (this) {
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
                    //this.mstatus = SessionMemStatus.NOT_LOADED;
                }
            }
        }
    }

    /**
     * Lock is released in couchbase. The lock is released and the session 
     * contents saved in the repo (the session can be saved completely or
     * just touched, it depends in access status).
     */
    @Override
    public void unlockForegroundCompletely() {
        log.fine("CouchbaseWrapperSession.unlockForegroundCompletely(): init");
        //in this case we are not using locks so just return true
        synchronized (this) {
            if (!isLocked()) {
                // not locked => just return
                return;
            }
            if (isForegroundLocked()) {
                // save the session
                doSave();
                // free the lock
                //this.mstatus = SessionMemStatus.NOT_LOADED;
                this.numForegroundLocks = 0;
            }
        }
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
            this.mstatus = SessionMemStatus.NOT_EXISTS;
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
    public boolean hasExpired() {
        boolean expired;
        synchronized (this) {
            if (isLocked()) {
                // session is currently locked => not expired
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
        }
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
        synchronized (this) {
            if (SessionAccessStatus.CLEAN.equals(this.astatus)) {
                this.astatus = SessionAccessStatus.ACCESSED;
            }
        }
    }
    
    /**
     * Remove attribute. Normal method but marking state as dirty.
     * @param name The name of the attribute to remove
     */
    @Override
    public void removeAttribute(String name) {
        super.removeAttribute(name);
        synchronized(this) {
            this.astatus = SessionAccessStatus.DIRTY;
        }
    }

    /**
     * Set attribute. Normal method but marking state as dirty.
     * @param name The name of the attribute to set
     * @param value The new value to set
     */
    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        synchronized(this) {
            this.astatus = SessionAccessStatus.DIRTY;
        }
    }
    
    /**
     * Debug method.
     * @return string representation of the session
     */
    @Override
    public String toString() {
        return new StringBuffer(this.getClass().getSimpleName())
                .append(" ")
                .append(this.mstatus)
                .append(" [")
                .append(getIdInternal())
                .append("] hash:")
                .append(Integer.toHexString(hashCode()))
                .append(" cas:")
                .append(cas)
                .toString();
    }
    
}