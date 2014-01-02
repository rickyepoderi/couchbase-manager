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

import com.couchbase.client.CouchbaseClient;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;

/**
 *
 * <p>Simple class that contains all the operations against couchbase server.
 * This class is intended to always use the asynchronous methods of the API,
 * although it can also be synchronous. Bulk operations haven been added
 * to manage external attributes in the session.</p>
 * 
 * @author ricky
 */
public class Client {
    
    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(Client.class.getName());
    
    /**
     * The couchbase client
     */
    private CouchbaseClient client = null;
    
    /**
     * The number of nodes to persist an operation.
     */
    private PersistTo persistTo = PersistTo.ZERO;
    
    /**
     * The number of nodes to replicate an operation.
     */
    private ReplicateTo replicateTo = ReplicateTo.ZERO;
    
    /**
     * Timeout for operations.
     */
    private long timeout = 30000L;
    
    /**
     * Constructor of the client. It uses the typical couchbase client 
     * arguments. persistTo=ZERO. replicateTo=ZERO. timeout=30000.
     * @param baseURIs The URIs where couchbase servers are
     * @param bucket The bucket for the sessions
     * @param username The username
     * @param password The password
     * @throws IOException Some error initializing the client
     */
    public Client(List<URI> baseURIs, String bucket, String username, 
            String password) throws IOException {
        client = new CouchbaseClient(baseURIs, bucket, username, password);
    }
    
    /**
     * Constructor of the client. It uses the typical couchbase client 
     * arguments. timeout=30000.
     * @param baseURIs The URIs where couchbase servers are
     * @param bucket The bucket for the sessions
     * @param username The username
     * @param password The password
     * @param persistTo  The number of nodes to persist
     * @param replicateTo The number of nodes to replicate
     * @throws IOException Some error initializing the client
     */
    public Client(List<URI> baseURIs, String bucket, String username, 
            String password, PersistTo persistTo, ReplicateTo replicateTo) throws IOException {
        this(baseURIs, bucket, username, password);
        this.persistTo = persistTo;
        this.replicateTo = replicateTo;
    }
    
    /**
     * Constructor of the client with all the properties.
     * @param baseURIsThe URIs where couchbase servers are
     * @param bucket The bucket for the sessions
     * @param username The username
     * @param password The password
     * @param persistTo  The number of nodes to persist
     * @param replicateTo The number of nodes to replicate
     * @param timeout The timeout for all operations
     * @throws IOException Some error creating the client
     */
    public Client(List<URI> baseURIs, String bucket, String username, 
            String password, PersistTo persistTo, ReplicateTo replicateTo, long timeout) throws IOException {
        this(baseURIs, bucket, username, password, persistTo, replicateTo);
        this.timeout = timeout;
    }

    /**
     * Getter for persistTo
     * @return The persistTo
     */
    public PersistTo getPersistTo() {
        return persistTo;
    }

    /**
     * Setter for persistTo
     * @param persistTo The new persistTo
     */
    public void setPersistTo(PersistTo persistTo) {
        this.persistTo = persistTo;
    }

    /**
     * Getter the replicateTo
     * @return The replicateTo
     */
    public ReplicateTo getReplicateTo() {
        return replicateTo;
    }

    /** 
     * Setter the replicateTo
     * @param replicateTo The new replicateTo
     */
    public void setReplicateTo(ReplicateTo replicateTo) {
        this.replicateTo = replicateTo;
    }

    /**
     * Getter for timeout.
     * @return The timeout for operations
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Setter for timeout.
     * @param timeout The timeout for operations
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    
    /**
     * Shutdowns the client.
     */
    public void shutdown() {
        client.shutdown();
    }
    
    /**
     * After one operation request is created this method waits synchronously 
     * the completion of the method. So although always asynch methods are 
     * executed this method gives the synch way of execution. Since v0.2 the
     * PersistTo and ReplicateTo parameters are also used to force a write
     * operation to persist or replicate to a number of nodes.
     * @param request The request to execute
     * @return The client result of the operation
     */
    public ClientResult waitForCompletion(ClientRequest request) {
        ClientResult response = request.waitForCompletion(timeout);
        if (response.isSuccess() && response.getCas() != -1 && (request.isOperation() || request.isCAS())) {
            // assure that all operations (set, cas, delete, add are waited)
            log.log(Level.FINE, "Doing the observePoll: {0} - {1}", 
                    new Object[]{request.getType(), response.getCas()});
            client.observePoll(response.getKey(), response.getCas(), 
                    persistTo, replicateTo, 
                    request.getType().equals(OperationType.DELETE));
        }
        return response;
    }
    
    /**
     * After one operation is created this method executes it asynchronously.
     * The exec parameter let us execute some code after the operation is
     * finished. This method launches the request using a thread and this thread
     * waits for the operation to finish and then executes the exec. Since v0.2
     * it waits to PersistTo and ReplicateTo.
     * @param request The request to execute async
     * @param exec The code to execute after the operation is performed (can be null)
     */
    protected void execOnCompletion(ClientRequest request, ExecOnCompletion exec) {
        request.execOnCompletion(this, exec);
    }
    
    /**
     * Method to execute a sync getAndLock operation.
     * @param id The data to receive
     * @param exp The expiration timeout
     * @return The result of this operation
     */
    public ClientResult getAndLockSync(String id, int exp) {
        ClientRequest req = ClientRequest.createGetAndLockResult(client.asyncGetAndLock(id, exp));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute a sync gets operation.
     * @param id The data to receive
     * @return The result of this operation
     */
    public ClientResult getsSync(String id) {
        ClientRequest req = ClientRequest.createGets(client.asyncGets(id));
        return req.waitForCompletion(timeout);
    }
    
    /**
     * Method to execute a sync set operation.
     * @param id The id of the object
     * @param data The serialized object to set
     * @param exp The new expiration time
     * @return The result of this operation
     */
    public ClientResult setSync(String id, byte[] data, int exp) {
        ClientRequest req = ClientRequest.createSet(client.set(id, exp, data));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute a set async.
     * @param id The id of the object
     * @param data The data to set
     * @param exp The expiration time
     * @param exec The exec to execute after the completion
     * @return The client request with the exec assigned
     */
    public ClientRequest setAsync(String id, byte[] data, int exp, ExecOnCompletion exec) {
        ClientRequest req = ClientRequest.createSet(client.set(id, exp, data));
        this.execOnCompletion(req, exec);
        return req;
    }
    
    /**
     * Method to execute a sync cas operation.
     * @param id The id of the object
     * @param data The serialized object to cas
     * @param cas The cas read previously
     * @param exp The new expiration time
     * @return The result of this operation
     */
    public ClientResult casSync(String id, byte[] data, long cas, int exp) {
        ClientRequest req = ClientRequest.createCas(
                client.asyncCAS(id, cas, exp, data, client.getTranscoder()));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute an async cas operation.
     * @param id The id of the object
     * @param data The data to cas
     * @param cas The cas to use
     * @param exp The expiration time
     * @param exec The exec to execute when the op finishes
     * @return The result of this operation
     */
    public ClientRequest casAsync(String id, byte[] data, long cas, int exp, ExecOnCompletion exec) {
        ClientRequest req = ClientRequest.createCas(
                client.asyncCAS(id, cas, exp, data, client.getTranscoder()));
        this.execOnCompletion(req, exec);
        return req;
    }
    
    /**
     * Method to execute a sync delete operation.
     * @param id The id to delete
     * @return The result of this operation
     */
    public ClientResult deleteSync(String id) {
        ClientRequest req = ClientRequest.createDelete(client.delete(id));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute an async delete operation.
     * @param id The id of the object
     * @param exec The exec to execute when the op finishes
     * @return The request created with the exec attached
     */
    public ClientRequest deleteAsync(String id, ExecOnCompletion exec) {
        ClientRequest req = ClientRequest.createDelete(client.delete(id));
        this.execOnCompletion(req, exec);
        return req;
    }
    
    /**
     * Method to execute a sync delete operation with cas (used when locked).
     * @param id The id of the obejct
     * @param cas The cas of the get
     * @return The result of this operation
     */
    public ClientResult deleteSync(String id, long cas) {
        ClientRequest req = ClientRequest.createDelete(client.delete(id, cas));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute an async delete operation with cas.
     * @param id The id of the object
     * @param cas The cas to use
     * @param exec The exec to execute when the op finishes
     * @return The client used with the exec attached
     */
    public ClientRequest deleteAsync(String id, long cas, ExecOnCompletion exec) {
        ClientRequest req = ClientRequest.createDelete(client.delete(id, cas));
        this.execOnCompletion(req, exec);
        return req;
    }
    
    /**
     * Method to execute a sync unlock operation.
     * @param id The id to unlock
     * @param cas The cas previously read
     * @return The result of this operation
     */
    public ClientResult unlockSync(String id, long cas) {
        ClientRequest req = ClientRequest.createUnlock(client.asyncUnlock(id, cas));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute an async unlock operation.
     * @param id The id of the object
     * @param cas The cas to use
     * @param exec The exec to execute whe the op finishes
     * @return The client created with the exec assigned
     */
    public ClientRequest unlockAsync(String id, long cas, ExecOnCompletion exec) {
        ClientRequest req = ClientRequest.createUnlock(client.asyncUnlock(id, cas));
        this.execOnCompletion(req, exec);
        return req;
    }
    
    /**
     * Method to execute a sync touch operation.
     * @param id The data to touch
     * @param exp The new expiration time
     * @return The result of this operation
     */
    public ClientResult touchSync(String id, int exp) {
        ClientRequest req = ClientRequest.createTouch(client.touch(id, exp));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute an async touch operation.
     * @param id The id of the object
     * @param exp The new expiration time
     * @param exec The exec to execute when it finishes
     * @return The request created with the exec attached
     */
    public ClientRequest touchAsync(String id, int exp, ExecOnCompletion exec) {
        ClientRequest req = ClientRequest.createTouch(client.touch(id, exp));
        this.execOnCompletion(req, exec);
        return req;
    }
    
    /**
     * Method to execute a aync add operation.
     * @param id The id of the object
     * @param data The serialized object to add
     * @param exp The new expiration time
     * @return The result of this operation
     */
    public ClientResult addSync(String id, byte[] data, int exp) {
        ClientRequest req = ClientRequest.createAdd(client.add(id, exp, data));
        return this.waitForCompletion(req);
    }
    
    /**
     * Method to execute an async operation.
     * @param id The object of the object
     * @param data The serialized object to add
     * @param exec The exec to execute when the op finishes
     * @return The request launched with the op attached
     */
    public ClientRequest addAsync(String id, byte[] data, int exp, ExecOnCompletion exec) {
        ClientRequest req = ClientRequest.createAdd(client.add(id, exp, data));
        this.execOnCompletion(req, exec);
        return req;
    }
    
    //
    // BULK OPS
    //
    
    /**
     * Method that creates an empty bulk request. It will be used with
     * all the rest of bulk methods.
     * @return The new bulk request
     */
    public BulkClientRequest createBulk() {
        return new BulkClientRequest();
    }
    
    /**
     * Method that adds a new add operation inside the bulk request.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param exp The expiration time
     */
    public void addOperationAdd(BulkClientRequest bulk, String id, byte[] data, int exp) {
        bulk.addOperation(ClientRequest.createAdd(client.add(id, exp, data)));
    }
    
    /**
     * Method that sets a new add operation inside the bulk request.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to cas
     * @param exp The expiration time
     */
    public void addOperationSet(BulkClientRequest bulk, String id, byte[] data, int exp) {
        bulk.addOperation(ClientRequest.createSet(client.set(id, exp, data)));
    }
    
    /**
     * Method that touches a new add operation inside the bulk request.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param exp The expiration time
     */
    public void addOperationTouch(BulkClientRequest bulk, String id, int exp) {
        bulk.addOperation(ClientRequest.createTouch(client.touch(id, exp)));
    }
    
    /**
     * Method that deletes with cas a new add operation inside the bulk request.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param cas The cas to use
     */
    public void addOperationDelete(BulkClientRequest bulk, String id, long cas) {
        bulk.addOperation(ClientRequest.createDelete(client.delete(id, cas)));
    }
    
    /**
     * Method that deletes without cas a new add operation inside the bulk request.
     * @param bulk The bulk to use
     * @param id The id of the object
     */
    public void addOperationDelete(BulkClientRequest bulk, String id) {
        bulk.addOperation(ClientRequest.createDelete(client.delete(id)));
    }
    
    // FINISH (ASYNC NO WAITING)
    
    /**
     * Finish a bulk operation with an ADD which is launched at the same
     * time, not waiting for the previous ops to finish. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param exp The expiration time
     * @param exec The exec to execute when bulk finishes
     */
    public void finishAddAsync(BulkClientRequest bulk, String id, 
            byte[] data, int exp, ExecOnCompletion exec) {
        bulk.finish(ClientRequest.createAdd(client.add(id, exp, data)));
        bulk.execOnCompletion(this, exec);
    }
    
    /**
     * Finish a bulk operation with a CAS which is launched at the same
     * time, not waiting for the previous ops to finish. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param cas The cas to use
     * @param exp The expiration time
     * @param exec The exec to execute when bulk finishes
     */
    public void finishCasAsync(BulkClientRequest bulk, String id, 
            byte[] data, long cas, int exp, ExecOnCompletion exec) {
        bulk.finish(ClientRequest.createCas(
                client.asyncCAS(id, cas, exp, data, client.getTranscoder())));
        bulk.execOnCompletion(this, exec);
    }
    
    /**
     * Finish a bulk operation with a SET which is launched at the same
     * time, not waiting for the previous ops to finish. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param exp The expiration time
     * @param exec The exec to execute when bulk finishes
     */
    public void finishSetAsync(BulkClientRequest bulk, String id, 
            byte[] data, int exp, ExecOnCompletion exec) {
        bulk.finish(ClientRequest.createSet(client.set(id, exp, data)));
        bulk.execOnCompletion(this, exec);
    }
    
    /**
     * Finish a bulk operation with a DELETE(cas) which is launched at the same
     * time, not waiting for the previous ops to finish. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param cas The cas to use
     * @param exec The exec to execute when bulk finishes
     */
    public void finishDeleteAsync(BulkClientRequest bulk,
            String id, long cas, ExecOnCompletion exec) {
        bulk.finish(ClientRequest.createDelete(client.delete(id, cas)));
        bulk.execOnCompletion(this, exec);
    }
    
    /**
     * Finish a bulk operation with a DELETE(no cas) which is launched at the same
     * time, not waiting for the previous ops to finish. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param exec The exec to execute when bulk finishes
     */
    public void finishDeleteAsync(BulkClientRequest bulk,
            String id, ExecOnCompletion exec) {
        bulk.finish(ClientRequest.createDelete(client.delete(id)));
        bulk.execOnCompletion(this, exec);
    }
    
    // FINISH (SYNC NO WAITING)
    
    /**
     * Finish a bulk operation with an ADD launched at the same time (there is
     * no waiting for the previous operations to finish). It is a sync method,
     * the result (first error or last result) is returned.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param exp The expiration time
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult finishAddSync(BulkClientRequest bulk, 
            String id, byte[] data, int exp) {
        bulk.finish(ClientRequest.createAdd(client.add(id, exp, data)));
        return bulk.waitForCompletion(this);
    }
    
    /**
     * Finish a bulk operation with a CAS launched at the same time (there is
     * no waiting for the previous operations to finish). It is a sync method,
     * the result (first error or last result) is returned.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param cas The cas to use
     * @param exp The expiration time
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult finishCasSync(BulkClientRequest bulk, 
            String id, byte[] data, long cas, int exp) {
        bulk.finish(ClientRequest.createCas(
                client.asyncCAS(id, cas, exp, data, client.getTranscoder())));
        return bulk.waitForCompletion(this);
    }
    
    /**
     * Finish a bulk operation with a SET launched at the same time (there is
     * no waiting for the previous operations to finish). It is a sync method,
     * the result (first error or last result) is returned.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to set
     * @param exp The expiration time
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult finishSetSync(BulkClientRequest bulk, 
            String id, byte[] data, int exp) {
        bulk.finish(ClientRequest.createSet(client.set(id, exp, data)));
        return bulk.waitForCompletion(this);
    }
    
    /**
     * Finish a bulk operation with a DELETE(cas) launched at the same time (there is
     * no waiting for the previous operations to finish). It is a sync method,
     * the result (first error or last result) is returned.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param cas The cas to use
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult finishDeleteSync(BulkClientRequest bulk,  
            String id, long cas) {
        bulk.finish(ClientRequest.createDelete(client.delete(id, cas)));
        return bulk.waitForCompletion(this);
    }
    
    /**
     * Finish a bulk operation with a DELETE(no cas) launched at the same time (there is
     * no waiting for the previous operations to finish). It is a sync method,
     * the result (first error or last result) is returned.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult finishDeleteSync(BulkClientRequest bulk, 
            String id) {
        bulk.finish(ClientRequest.createDelete(client.delete(id)));
        return bulk.waitForCompletion(this);
    }
    
    // FINISH (ASYNC AND WAITING)
    
    /**
     * Wait for previous ops and then launches the add. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param exp The expiration time
     * @param exec The exec to execute when bulk finishes
     */
    public void waitAndFinishAddAsync(BulkClientRequest bulk,  
            String id, byte[] data, int exp, ExecOnCompletion exec) {
        bulk.finishAdd(this, id, data, exp, exec);
    }
    
    /**
     * Wait for previous ops and then launches the CAS. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to cas
     * @param cas The cas to use
     * @param exp The expiration time
     * @param exec The exec to use when bulk finishes
     */
    public void waitAndFinishCasAsync(BulkClientRequest bulk, 
            String id, byte[] data, long cas, int exp, ExecOnCompletion exec) {
        bulk.finishCas(this, id, data, cas, exp, exec);
    }
    
    /**
     * Wait for previous ops and then launches the SET. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to set
     * @param exp The expiration time
     * @param exec The exec to use when bulk finishes
     */
    public void waitAndFinishSetAsync(BulkClientRequest bulk, 
            String id, byte[] data, int exp, ExecOnCompletion exec) {
        bulk.finishSet(this, id, data, exp, exec);
    }
    
    /**
     * Wait for previous ops and then launches the DELETE with cas. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param cas The cas to use
     * @param exec The exec to use when bulk finishes
     */
    public void waitAndFinishDeleteAsync(BulkClientRequest bulk, String id, 
            long cas, ExecOnCompletion exec) {
        bulk.finishDelete(this, id, cas, exec);
    }
    
    /**
     * Wait for previous ops and then launches the DELETE without cas. It is async.
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param exec The exec to use when bulk finishes
     */
    public void waitAndFinishDeleteAsync(BulkClientRequest bulk, String id, ExecOnCompletion exec) {
        bulk.finishDelete(this, id, exec);
    }
    
    // FINISH (SYNC AND WAITING)
    
    /**
     * Wait for previous ops, then launches the ADD and finally wait it. 
     * So it is sync and the result is returned (first error or last result).
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to add
     * @param exp The expiration time
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult waitAndFinishAddSync(BulkClientRequest bulk, 
            String id, byte[] data, int exp) {
        ClientResult res = bulk.waitForOpsCompletion(this);
        if (res == null || res.isSuccess()) {
            bulk.finish(ClientRequest.createAdd(client.add(id, exp, data)));
            res = bulk.waitForLastCompletion(this);
        }
        return res;
    }
    
    /**
     * Wait for previous ops, then launches the CAS and finally wait it. 
     * So it is sync and the result is returned (first error or last result).
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to cas
     * @param cas The cas to use
     * @param exp The expiration time
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult waitAndFinishCasSync(BulkClientRequest bulk, 
            String id, byte[] data, long cas, int exp) {
        ClientResult res = bulk.waitForOpsCompletion(this);
        if (res == null || res.isSuccess()) {
            bulk.finish(ClientRequest.createCas(
                client.asyncCAS(id, cas, exp, data, client.getTranscoder())));
            res = bulk.waitForLastCompletion(this);
        }
        return res;
    }
    
    /**
     * Wait for previous ops, then launches the SET and finally wait it. 
     * So it is sync and the result is returned (first error or last result).
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param data The serialized object to set
     * @param exp The expiration time
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult waitAndFinishSetSync(BulkClientRequest bulk, 
            String id, byte[] data, int exp) {
        ClientResult res = bulk.waitForOpsCompletion(this);
        if (res == null || res.isSuccess()) {
            bulk.finish(ClientRequest.createSet(client.set(id, exp, data)));
            res = bulk.waitForLastCompletion(this);
        }
        return res;
    }
    
    /**
     * Wait for previous ops, then launches the DELETE with cas and finally wait it. 
     * So it is sync and the result is returned (first error or last result).
     * @param bulk The bulk to use
     * @param id The id of the object
     * @param cas The cas to use
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult waitAndFinishDeleteSync(BulkClientRequest bulk, 
            String id, long cas) {
        ClientResult res = bulk.waitForOpsCompletion(this);
        if (res == null || res.isSuccess()) {
            bulk.finish(ClientRequest.createDelete(client.delete(id, cas)));
            res = bulk.waitForLastCompletion(this);
        }
        return res;
    }
    
    /**
     * Wait for previous ops, then launches the DELETE without cas and finally wait it. 
     * So it is sync and the result is returned (first error or last result).
     * @param bulk The bulk to use
     * @param id The id of the object
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult waitAndFinishDeleteSync(BulkClientRequest bulk, 
            String id) {
        ClientResult res = bulk.waitForOpsCompletion(this);
        if (res == null || res.isSuccess()) {
            bulk.finish(ClientRequest.createDelete(client.delete(id)));
            res = bulk.waitForLastCompletion(this);
        }
        return res;
    }
    
    /**
     * It waits a bulk operation to finish. It should be finished async.
     * @param bulk The bulk operation
     * @return The result of the bulk (first error or last result)
     */
    public ClientResult waitForCompletion(BulkClientRequest bulk) {
        return bulk.waitForCompletion(this);
    }
}