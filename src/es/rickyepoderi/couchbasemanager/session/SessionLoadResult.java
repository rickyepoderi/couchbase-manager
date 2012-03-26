/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.session;

import es.rickyepoderi.couchbasemanager.session.CouchbaseWrapperSession.SessionMemStatus;

/**
 * <p>Utility class to store the result from load of a session from the 
 * repository. It just holds the session itself, the cas result and the 
 * status (OK, BLOCKED, NOT_EXISTS and so on).</p>
 * 
 * @author ricky
 */
public class SessionLoadResult {
    /*
     * The session read (can be null)
     */
    private CouchbaseWrapperSession session = null;
    
    /**
     * The CAS obtained from gets or getAndLock
     */
    private long cas = -1;
    
    /**
     * Status of the read: OK, NOT_EXISTS, BLOCKED,...
     */
    private SessionMemStatus mstatus = null;

    //
    // CONSTRUCTORS
    //
    
    /**
     * Empty constructor.
     */
    public SessionLoadResult() {
    }
    
    /**
     * Constructor via the CAS.
     * @param cas The CAS read from the repo.
     * @param status Status of the load
     */
    public SessionLoadResult(long cas, SessionMemStatus status) {
        this.cas = cas;
        this.mstatus = status;
    }
    
    /**
     * Constructor via CAS, status and session.
     * @param cas The CAS of the read
     * @param status The status of the read
     * @param session The session loaded
     */
    public SessionLoadResult(long cas, SessionMemStatus status, CouchbaseWrapperSession session) {
        this.cas = cas;
        this.mstatus = status;
        this.session = session;
    }
    
    /**
     * Getter for CAS.
     * @return CAS stored.
     */
    public long getCas() {
        return cas;
    }

    /**
     * Setter for CAS.
     * @param cas the new CAS.
     */
    public void setCas(long cas) {
        this.cas = cas;
    }

    /**
     * Getter for session.
     * @return The session loaded.
     */
    public CouchbaseWrapperSession getSession() {
        return session;
    }

    /**
     * Setter for the session.
     * @param session The new session.
     */
    public void setSession(CouchbaseWrapperSession session) {
        this.session = session;
    }

    /**
     * Getter for the status.
     * @return The status of the read.
     */
    public SessionMemStatus getMemStatus() {
        return mstatus;
    }

    /**
     * Setter for the status.
     * @param status The new status
     */
    public void setMemStatus(SessionMemStatus status) {
        this.mstatus = status;
    }
    
    /**
     * String representation of the result.
     * @return The string representation.
     */
    @Override
    public String toString() {
        return new StringBuffer()
                .append(mstatus)
                .append("(")
                .append(cas)
                .append(")")
                .append("->")
                .append(session)
                .toString();
    }
}
