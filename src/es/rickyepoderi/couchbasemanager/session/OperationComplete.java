/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.session;

import es.rickyepoderi.couchbasemanager.couchbase.ClientResult;
import es.rickyepoderi.couchbasemanager.couchbase.ExecOnCompletion;
import es.rickyepoderi.couchbasemanager.couchbase.OperationType;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Simple class that executes the code of the couchbase session after an
 * asynch method is finished. If the method was to save the session, the session
 * is cleared, if the method was refreshed is also cleared, if deleted the session
 * is deleted from the internal session list and otherwise the session request
 * is set to null.
 * 
 * @author ricky
 */
public class OperationComplete implements ExecOnCompletion {

    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(OperationComplete.class.getName());
    
    /**
     * The session of the request
     */
    private CouchbaseWrapperSession session = null;

    /**
     * Constructor via session.
     * @param session The session
     */
    public OperationComplete(CouchbaseWrapperSession session) {
        this.session = session;
    }

    /**
     * Method that clears the session after the asynch method is executed.
     * @param result The result of the operation.
     */
    @Override
    public void execute(ClientResult result) {
        log.log(Level.FINE, "OperationComplete.execute(ClientResult): init {0}", result);
        if (OperationType.DELETE.equals(result.getType())) {
            log.fine("OperationComplete.execute(ClientResult): delete session completely");
            if (result.isSuccess()) {
                ((CouchbaseManager)session.getManager()).realRemove(session);
            }
        } else {
            log.fine("OperationComplete.execute(ClientResult): clear");
        }
        session.clearRequestAndNotify(result);
    }
}
