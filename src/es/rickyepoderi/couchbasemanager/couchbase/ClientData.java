/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.couchbase;

/**
 *
 * A simple interface that is used in the Client package. The Couchbase session
 * class should implement this interface.
 * 
 * @author ricky
 */
public interface ClientData {
    
    /**
     * The id used as key inside couchbase.
     * @return The id of the data
     */
    public String getId();
    
}
