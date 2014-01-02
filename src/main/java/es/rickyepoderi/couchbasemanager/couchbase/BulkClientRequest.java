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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>The bulk request is a representation for doing several requests to
 * couchbase at the same time. The idea consists in launching all the operations
 * (those ops are immediately started) and then finish with a last operation.
 * At that point you can wait all to finish. Only the response for the last
 * operation is returned if success. In case of error in any operation the
 * error result is returned.</p>
 * 
 * <p>The bulk operation is thought to be used with the session external 
 * attributes:</p>
 * 
 * <ul>
 * <li>The external attribute operations are performed first (adding a new
 * one, setting a new value or deleting one of them).</li>
 * <li>All the operations start to execute as soon as they are added to
 * the bulk object.</li>
 * <li>The final operation is the real session operation (cas, set, add or 
 * delete).</li>
 * </ul>
 * 
 * <p>There is another weird case, if the manager is configured non-sticky 
 * the last cas means the unlocking of the couchbase of the object. It is
 * compulsory that all the external attribute operations have been finished
 * before the unlock. For that reason the last operation can be launched
 * at the same time of the previous or once they are finished.</p>
 * 
 * @author ricky
 */
public class BulkClientRequest {
    
    /**
     * Inner thread to launch async operations.
     */
    private Thread thread = null;
    
    /**
     * The executor (used to read the last response from the thread).
     */
    private Executor executor = null;
    
    /**
     * The list of requests launched before the last one.
     */
    private List<ClientRequest> ops = null;
    
    /**
     * The last client request when launched at the same time.
     */
    private ClientRequest last = null;
    
    /**
     * The type of the last operation when launched after the previous ops.
     */
    private OperationType type = null;
    
    /**
     * The id of the last operation when launched after the previous ops.
     */
    private String id = null;
    
    /**
     * The data of the last operation when launched after the previous ops.
     */
    private byte[] data = null;
    
    /**
     * The cas of the last operation when launched after the previous ops.
     */
    private Long cas = null;
    
    /**
     * The time of the last operation when launched after the previous ops.
     */
    private Integer time = null;
    
    /**
     * The executor runnable to wait the operation to finish.
     */
    private class Executor implements Runnable {

        /**
         * The client used to launch ops.
         */
        private Client client = null;
        
        /**
         * The bulk used.
         */
        private BulkClientRequest bulk = null;
        
        /**
         * The exec to execute at the end.
         */
        private ExecOnCompletion exec = null;
        
        /**
         * The result to return.
         */
        private ClientResult res = null;
        
        /**
         * Constructor using the operations.
         * @param client The client to use
         * @param bulk The bulk to use
         * @param exec The exec op
         */
        public Executor(Client client, BulkClientRequest bulk, ExecOnCompletion exec) {
            this.client = client;
            this.bulk = bulk;
            this.exec = exec;
            this.res = null;
        }
        
        /**
         * Getter for the result.
         * @return The result
         */
        public ClientResult getClientResult() {
            return res;
        }
        
        /**
         * Method that wait for the previous operations, then (if the last
         * is already launched) it waits for the last one. If the last one
         * is marked to be launched after waiting the previous ones, it is
         * launched and then waited. If during the process some operation
         * return error, the process stops and this response is set.
         */
        @Override
        public void run() {
            // first wait for all the ops
            res = bulk.waitForOpsCompletion(client);
            // now we have to finish the last operation
            // which can be already launched or the operation should
            // wait for the previous ops to terminate (CAS mainly)
            if (res == null || res.isSuccess()) {
                if (bulk.isLastExecuted()) {
                    // already launched => just wait too
                    res = bulk.waitForLastCompletion(client);
                } else {
                    // launch last
                    res = bulk.launchAndWaitLastOperation(client);
                }
            }
            if (exec != null) {
                exec.execute(res);
            }
        }
    }
    
    /**
     * Empty constructor.
     */
    protected BulkClientRequest() {
        this.last = null;
        this.ops = new ArrayList<ClientRequest>();
    }
    
    /**
     * Method that return if the last operation is executed with the others
     * (last is used) or after the others (type and the rest of props are used).
     * @return true if the last operation was executed at the same time, false
     *         otherwise.
     */
    protected boolean isLastExecuted() {
        // the last operation was executed not waiting for the
        // ops previously if type is null;
        return this.type == null;
    }
    
    /**
     * This method adds a new operation which was launched at creation.
     * @param op The op to add to the list of launched ops.
     */
    protected void addOperation(ClientRequest op) {
        if (this.last != null || this.type != null) {
            throw new IllegalStateException("A new operation cannot be added after finish!");
        }
        this.ops.add(op);
    }
    
    /**
     * Finish the bulk operation with another operation. This operation is also
     * launched at the same time.
     * @param last The last operation launched
     */
    protected void finish(ClientRequest last) {
        if (this.last != null || this.type != null) {
            throw new IllegalStateException("Already finished!");
        }
        this.last = last;
    }
    
    /**
     * The last operation is an ADD but it will be launched when the previous 
     * finish.
     * @param client The client used to launch the op
     * @param id The id to add
     * @param data The data to add
     * @param time The expiration time
     * @param exec The exec to execute at finish
     */
    protected void finishAdd(Client client, String id, byte[] data, int time, 
            ExecOnCompletion exec) {
        if (this.last != null || this.type != null) {
            throw new IllegalStateException("Already finished!");
        }
        this.type = OperationType.ADD;
        this.id = id;
        this.data = data;
        this.time = time;
        execOnCompletion(client, exec);
    }
    
    /**
     * The last operation is a CAS but it will be launched when the previous 
     * finish.
     * @param client The client used to launch the op
     * @param id The id to add
     * @param data The data to add
     * @param cas The cas for the operation
     * @param time The expiration time
     * @param exec The exec to execute at finish
     */
    protected void finishCas(Client client, String id, byte[] data, long cas, 
            int time, ExecOnCompletion exec) {
        if (this.last != null || this.type != null) {
            throw new IllegalStateException("Already finished!");
        }
        this.type = OperationType.CAS;
        this.id = id;
        this.data = data;
        this.cas = cas;
        this.time = time;
        execOnCompletion(client, exec);
    }
    
    /**
     * The last operation is a SET but it will be launched when the previous 
     * finish.
     * @param client The client used to launch the op
     * @param id The id to add
     * @param data The data to add
     * @param time The expiration time
     * @param exec The exec to execute at finish
     */
    protected void finishSet(Client client, String id, byte[] data, int time, 
            ExecOnCompletion exec) {
        if (this.last != null || this.type != null) {
            throw new IllegalStateException("Already finished!");
        }
        this.type = OperationType.SET;
        this.id = id;
        this.data = data;
        this.time = time;
        execOnCompletion(client, exec);
    }
    
    /**
     * The last operation is a DELETE but it will be launched when the previous 
     * finish (cas used).
     * @param client The client used to launch the op
     * @param id The id to add
     * @param cas The cas fir the delete
     * @param exec The exec to execute at finish
     */
    protected void finishDelete(Client client, String id, long cas, ExecOnCompletion exec) {
        if (this.last != null || this.type != null) {
            throw new IllegalStateException("Already finished!");
        }
        this.type = OperationType.DELETE;
        this.id = id;
        this.cas = cas;
        execOnCompletion(client, exec);
    }
    
    /**
     * The last operation is a DELETE but it will be launched when the previous 
     * finish (no cas used).
     * @param client The client used to launch the op
     * @param id The id to add
     * @param exec The exec to execute at finish
     */
    protected void finishDelete(Client client, String id, ExecOnCompletion exec) {
        if (this.last != null || this.type != null) {
            throw new IllegalStateException("Already finished!");
        }
        this.type = OperationType.DELETE;
        this.id = id;
        execOnCompletion(client, exec);
    }
    
    /**
     * Intermediate method that launches the last operation when it was
     * specified as later op. The op is launched synchronously so the 
     * response is waited and returned.
     * @param client The client to launch the operation
     * @return The response for the last operation
     */
    protected ClientResult launchAndWaitLastOperation(Client client) {
        ClientResult res;
        // launch the operation once the previous ops are finished
        switch (this.type) {
            case ADD:
                res = client.addSync(this.id, this.data, this.time);
                break;
            case SET:
                res = client.setSync(this.id, this.data, this.time);
                break;
            case CAS:
                res = client.casSync(this.id, this.data, this.cas, this.time);
                break;
            case DELETE:
                if (this.cas != null) {
                    res = client.deleteSync(this.id, this.cas);
                } else {
                    res = client.deleteSync(this.id);
                }
                break;
            default:
                res = ClientResult.createClientResultError(type, 
                        new IllegalAccessException(String.format("Illegal last operation: %s!", type.toString())));
                break;
        }
        return res;
    }
    
    /**
     * Method that waits last operation using the client.waitForCompletion.
     * @param client The client to use
     * @return The response for the last operation
     */
    protected ClientResult waitForLastCompletion(Client client) {
        return client.waitForCompletion(this.last);
    }
    
    /**
     * Method that iterate over all the previous ops and waits them to complete.
     * @param client The client used for waiting
     * @return null in case no previous operation exists, the first error
     *         response or the last success.
     */
    protected ClientResult waitForOpsCompletion(Client client) {
        ClientResult res = null;
        // wait all the operations to finish
        for (ClientRequest req : this.ops) {
            res = client.waitForCompletion(req);
            if (!res.isSuccess()) {
                break;
            }
        }
        return res;
    }
    
    /**
     * Method that waits for completion no matter if the last operation
     * was executed at the same time or after the previous one. If a thread
     * was used this method joins to the thread, if no thread was used just normal
     * waitForCompletion is used. Remember that if any op returns an error 
     * the precess stops and that error is returned.
     * @param client The client to use
     * @return if any previous operation is an error that error response, if 
     *         not the last operation result.
     */
    protected ClientResult waitForCompletion(Client client) {
        if (this.last == null && this.type == null) {
            throw new IllegalStateException("Cannot wait an unfinished operation!");
        }
        ClientResult res;
        // the bulk can be executed at once (addOperation and finish)
        if (this.isLastExecuted()) {
            // the bulk was all launched with no thread
            res = this.waitForOpsCompletion(client);
            if (res == null || res.isSuccess()) {
                res = this.waitForLastCompletion(client);
            }
        } else {
            // the last operation was delayed to the end of ops
            // join the thread at wait it to finish
            try {
                thread.join(client.getTimeout());
            } catch(InterruptedException e) {}
            if (thread.isAlive()) {
                res = ClientResult.createClientResultError(type, 
                        new Exception("Thread is still alive!"));
            } else {
                res = executor.getClientResult();
            }
        }
        return res;
    }
    
    /**
     * Method that launches the executor thread in case the operation should
     * be executed asynchronously.
     * @param client The client to execute
     * @param exec The exec to launch after bulk finishes
     */
    protected void execOnCompletion(Client client, ExecOnCompletion exec) {
        if (this.last == null && this.type == null) {
            throw new IllegalStateException("Cannot exec an unfinished operation!");
        }
        if (thread == null) {
            executor = new Executor(client, this, exec);
            thread = new Thread(executor);
            thread.start();
        }
    }
}
