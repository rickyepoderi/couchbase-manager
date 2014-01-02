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
package es.rickyepoderi.couchbasemanager.couchbase;

import java.util.concurrent.Future;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.OperationFuture;

/**
 *
 * A request client is a class that encapsulates all possible operations that
 * can be done against the couchbase server. The class creates any operation
 * and can wait synchronously or asynchronously the operation to finish.
 * 
 * @author ricky
 */
public class ClientRequest {
    
    /**
     * Internal thread that waits the operation to finish.
     */
    private class Executor implements Runnable {

        /**
         * The client should be used cos request waitForCompletion now 
         * does not fit the PersistTo and ReplicateTo parameters.
         */
        private Client client = null;
        
        /**
         * The request.
         */
        private ClientRequest req = null;
        
        /**
         * The response.
         */
        private ClientResult res = null;
        
        /**
         * The code to execute at finish.
         */
        private ExecOnCompletion exec = null;
        
        /**
         * Constructor.
         * @param client The client
         * @param req The request
         * @param exec The code to execute (it can be null)
         */
        public Executor(Client client, ClientRequest req, ExecOnCompletion exec) {
            this.client = client;
            this.req = req;
            this.exec = exec;
        }
        
        /**
         * Return the result of the operation.
         * @return The result of the operation when finished.
         */
        public ClientResult getClientResult() {
            return res;
        }
        
        /**
         * The method waits the request to complete and then
         * execute the code if passed.
         */
        @Override
        public void run() {
            res = client.waitForCompletion(req);
            if (exec != null) {
                exec.execute(res);
            }
        }
    
    }
    
    /**
     * The operation of the request
     */
    OperationType type = null;
    
    /**
     * GET_AND_LOCK & GETS operations produce a OperationFuture&lt;CASValue&lt;Object&gt;&gt;
     * object.
     */
    private OperationFuture<CASValue<Object>> futureObject = null;
    
    /**
     * CAS is the only method that produces a OperationFuture&lt;CASResponse&gt; object.
     */
    private OperationFuture<CASResponse> futureCas = null;
    
    /**
     * All the rest of couchbase operations produces a OperationFuture&lt;Boolean&gt;.
     */
    private OperationFuture<Boolean> futureOperation = null;
    
    /**
     * The thread that waits for completion when asynchronous executed.
     */
    private Thread thread = null;
    
    /**
     * Constructor for the client request. It is private cos it can only be called 
     * using static methods which pass the correct futures.
     * @param type The type of the client
     * @param future The future returned by couchbase
     */
    protected ClientRequest(OperationType type, OperationFuture future) {
        this.type = type;
        if (OperationType.GET_AND_LOCK.equals(type) || OperationType.GETS.equals(type)) {
            this.futureObject = (OperationFuture) future;
        } else if (OperationType.CAS.equals(type)) {
            this.futureCas = future;
        } else {
            this.futureOperation = (OperationFuture) future;
        }
    }
    
    /**
     * createGetAndLockResult couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createGetAndLockResult(OperationFuture<CASValue<Object>> future) {
        return new ClientRequest(OperationType.GET_AND_LOCK, future);
    }
    
    /**
     * createGets couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createGets(OperationFuture<CASValue<Object>> future) {
        return new ClientRequest(OperationType.GETS, future);
    }
    
    /**
     * createSet couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createSet(OperationFuture<Boolean> future) {
        return new ClientRequest(OperationType.SET, future);
    }
    
    /**
     * createCas couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createCas(OperationFuture<CASResponse> future) {
        return new ClientRequest(OperationType.CAS, future);
    }
    
    /**
     * createDelete couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createDelete(OperationFuture<Boolean> future) {
        return new ClientRequest(OperationType.DELETE, future);
    }
    
    /**
     * createUnlock couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createUnlock(OperationFuture<Boolean> future) {
        return new ClientRequest(OperationType.UNLOCK, future);
    }
    
    /**
     * createTouch couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createTouch(OperationFuture<Boolean> future) {
        return new ClientRequest(OperationType.TOUCH, future);
    }
    
    /**
     * createAdd couchbase operation.
     * @param future The result of this couchbase operation
     * @return The request
     */
    protected static ClientRequest createAdd(OperationFuture<Boolean> future) {
        return new ClientRequest(OperationType.ADD, future);
    }

    /**
     * Get the type of the request.
     * @return The type
     */
    public OperationType getType() {
        return type;
    }
    
    /**
     * Check if the operation returns a object.
     * @return true is the operation returns an object, false otherwise
     */
    public boolean isObject() {
        return this.futureObject != null;
    }
    
    /**
     * Check if the operation returns a CAS.
     * @return true is the operation returns a CAS, false otherwise
     */
    public boolean isCAS() {
        return this.futureCas != null;
    }
    
    /**
     * Check if the operation returns a boolean operation.
     * @return true is the operation returns a boolean, false otherwise
     */
    public boolean isOperation() {
        return this.futureOperation != null;
    }
    
    /**
     * Return the object
     * @return the object or null
     */
    public OperationFuture<CASValue<Object>> getFutureObject() {
        return this.futureObject;
    }
    
    /**
     * Return the operation
     * @return the operation or null
     */
    public OperationFuture<Boolean> getFutureOperation() {
        return this.futureOperation;
    }
    
    /**
     * Return the CAS
     * @return the CAS or null
     */
    public Future<CASResponse> getFutureCAS() {
        return this.futureCas;
    }
    
    /**
     * Method that waits the operation to complete synchronously. The 
     * method is protected cos the Client one should be used, now PersistTo
     * and ReplicateTo parameters can be specified to force the operation
     * to be in a specified number of nodes.
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @return The client result of the operation
     */
    protected ClientResult waitForCompletion(long timeout) {
        if (this.isObject()) {
            return ClientResult.createClientResultObject(timeout, this.type, this.futureObject);
        } else if (this.isCAS()) {
            return ClientResult.createClientResultCas(timeout, this.type, this.futureCas);
        } else {
            return ClientResult.createClientResultOperation(timeout, this.type, this.futureOperation);
        }
    }
    
    /**
     * Executes the operation asynch but after it is finished a code is
     * executed. This method uses an internal thread.
     * @param client The client to be used to wait for the operation
     * @param exec The code to execute at finishing
     */
    protected void execOnCompletion(Client client, ExecOnCompletion exec) {
        if (thread == null) {
            thread = new Thread(new Executor(client, this, exec));
            thread.start();
        }
    }
    
    /**
     * Returns if there is an operation in progress. The operation is in
     * progress if the thread is alive.
     * @return true if there is an operation is progress.
     */
    public boolean isExecuting() {
        return (thread != null) && thread.isAlive();
    }
    
    /**
     * String representation.
     * @return The representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(": ");
        sb.append(this.type);
        return sb.toString();
    }
    
}
