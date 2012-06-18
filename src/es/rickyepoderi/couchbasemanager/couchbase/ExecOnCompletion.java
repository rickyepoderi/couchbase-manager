/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.couchbase;

/**
 *
 * Simple interface that is used to execute some code after an asynchronous
 * operation ends. This class is passed to request method in order to execute
 * some code (normally clear the session or whatever).
 * 
 * @author ricky
 */
public interface ExecOnCompletion {
    
    /**
     * The code to execute at finishing time.
     * @param result The result of the operation in couchbase.
     */
    public void execute(ClientResult result);
    
}
