/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.couchbase;

/**
 *
 * ENUM that list all the operation that currently the couchbase manager
 * is using. The pity is that some operation like deleteWithCas (delete a
 * locked object) or touchAndUnlock (touch and release a lock over an object)
 * are not implemented in couchbase API. The problem is two operations should be
 * executed instead.
 * 
 * @author ricky
 */
public enum OperationType {

    // Object operation, returns OperationFuture<CASValue<Object>>
    
    GET_AND_LOCK,
    GETS,
    
    // CAS operation, returns Future<CASResponse>
    
    CAS,
    
    // Common operation, returns OperationFuture<Boolean>
    
    SET,
    DELETE,
    UNLOCK,
    TOUCH,
    ADD
}
