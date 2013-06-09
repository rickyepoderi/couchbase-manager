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
import com.couchbase.client.CouchbaseConnectionFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.transcoders.Transcoder;

/**
 *
 * <p>Simple class that contains all the operations against couchbase server.
 * This class is intended to always use the asynchronous methods of the API,
 * although it can also be synchronous.</p>
 * 
 * @author ricky
 */
public class Client<Data extends ClientData> {
    
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
     * Constructor of the client. It uses the typical couchbase client 
     * arguments.
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
     * arguments and the transcoder to use.
     * @param baseURIs The URIs where couchbase servers are
     * @param bucket The bucket for the sessions
     * @param username The username
     * @param password The password
     * @param transcoder The transcoder to use in serialization/deserialization
     * @throws IOException Some error initializing the client
     */
    public Client(List<URI> baseURIs, String bucket, String username, 
            String password, Transcoder<Object> transcoder) throws IOException {
        CouchbaseConnectionFactory cf = new AppCouchbaseConnectionFactory(baseURIs, bucket, password, transcoder);
        client = new CouchbaseClient(cf);
    }
    
    /**
     * Constructor of the client. It uses the typical couchbase client 
     * arguments and the transcoder to use.
     * @param baseURIs The URIs where couchbase servers are
     * @param bucket The bucket for the sessions
     * @param username The username
     * @param password The password
     * @param transcoder The transcoder to use in serialization/deserialization
     * @param persistTo  The number of nodes to persist
     * @param replicateTo The number of nodes to replicate
     * @throws IOException Some error initializing the client
     */
    public Client(List<URI> baseURIs, String bucket, String username, 
            String password, Transcoder<Object> transcoder, 
            PersistTo persistTo, ReplicateTo replicateTo) throws IOException {
        CouchbaseConnectionFactory cf = new AppCouchbaseConnectionFactory(baseURIs, bucket, password, transcoder);
        client = new CouchbaseClient(cf);
        this.persistTo = persistTo;
        this.replicateTo = replicateTo;
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
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @return The client result of the operation
     */
    public ClientResult waitForCompletion(ClientRequest request, long timeout) {
        ClientResult response = request.waitForCompletion(timeout);
        if (response.isSuccess() && response.getCas() != -1 && (request.isOperation() || request.isCAS())) {
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
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @param exec The code to execute after the operation is performed (can be null)
     */
    public void execOnCompletion(ClientRequest request, long timeout, ExecOnCompletion exec) {
        request.execOnCompletion(this, timeout, exec);
    }
    
    /**
     * Method to create a getAndLock operation.
     * @param data The data to receive
     * @param timeout The timeout of the lock
     * @return The request of this operation
     */
    public ClientRequest getAndLock(Data data, int timeout) {
        return ClientRequest.createGetAndLockResult(client.asyncGetAndLock(data.getId(), timeout));
    }
    
    /**
     * Method to create a gets operation.
     * @param data The data to receive
     * @return The request of this operation
     */
    public ClientRequest gets(Data data) {
        return ClientRequest.createGets(client.asyncGets(data.getId()));
    }
    
    /**
     * Method to create a set operation.
     * @param data The data to set
     * @param time The new expiration time
     * @return The request of this operation
     */
    public ClientRequest set(Data data, int time) {
        return ClientRequest.createSet(client.set(data.getId(), time, data));
    }
    
    /**
     * Method to create a cas operation.
     * @param data The data to set
     * @param cas The cas read previously
     * @param time The new expiration time
     * @return The request of this operation
     */
    public ClientRequest cas(Data data, long cas, int time) {
        return ClientRequest.createCas(
                client.asyncCAS(data.getId(), cas, time, data, client.getTranscoder()));
    }
    
    /**
     * Method to create a delete operation.
     * @param data The data to delete
     * @return The request of this operation
     */
    public ClientRequest delete(Data data) {
        return ClientRequest.createDelete(client.delete(data.getId()));
    }
    
    /**
     * Method to create a unlock operation.
     * @param data The data to unlock
     * @param cas The cas previously read
     * @return The request of this operation
     */
    public ClientRequest unlock(Data data, long cas) {
        return ClientRequest.createUnlock(client.asyncUnlock(data.getId(), cas));
    }
    
    /**
     * Method to create a touch operation.
     * @param data The data to touch
     * @param time The new expiration time
     * @return The request of this operation
     */
    public ClientRequest touch(Data data, int time) {
        return ClientRequest.createTouch(client.touch(data.getId(), time));
    }
    
    /**
     * Method to create an add operation.
     * @param data The data to add
     * @param time The new expiration time
     * @return The request of this operation
     */
    public ClientRequest add(Data data, int time) {
        return ClientRequest.createAdd(client.add(data.getId(), time, data));
    }
    
    /**
     * Sample class for main method
     */
    static private class SampleData implements ClientData, Serializable {
        
        String id = null;
        
        public SampleData() {
            this.id = UUID.randomUUID().toString();
        }
        
        @Override
        public String getId() {
            return this.id;
        }
        
        @Override
        public String toString() {
            return this.id;
        }
        
    }
    
    /**
     * test
     * @param args
     * @throws Exception 
     */
    static public void main(String[] args) throws Exception {
        Client<SampleData> client = null;
        try {
            URI uri = new URI("http://localhost:8091/pools");
            List<URI> baseUris = new ArrayList<URI>();
            baseUris.add(uri);
            client = new Client<SampleData>(baseUris, "default", null, "");
            SampleData data = new SampleData();
            // add
            System.err.println(System.currentTimeMillis() + " Adding... " + data);
            ClientRequest addRequest = client.add(data, 30000);
            System.err.println(System.currentTimeMillis());
            ClientResult addResult = client.waitForCompletion(addRequest, 15000);
            System.err.println(System.currentTimeMillis() + " " + addResult.getStatus());
            // touch
            System.err.println(System.currentTimeMillis() + " Touch... " + data);
            ClientRequest touchRequest = client.touch(data, 30000);
            System.err.println(System.currentTimeMillis());
            ClientResult touchResult = client.waitForCompletion(touchRequest, 15000);
            System.err.println(System.currentTimeMillis() + " " + touchResult.getStatus());
            // set
            System.err.println(System.currentTimeMillis() + " Set... " + data);
            ClientRequest setRequest = client.touch(data, 30000);
            System.err.println(System.currentTimeMillis());
            ClientResult setResult = client.waitForCompletion(setRequest, 15000);
            System.err.println(System.currentTimeMillis() + " " + setResult.getStatus());
            // gets
            System.err.println(System.currentTimeMillis() + " Gets... " + data);
            ClientRequest getsRequest = client.gets(data);
            System.err.println(System.currentTimeMillis());
            ClientResult getsResult = client.waitForCompletion(getsRequest, 15000);
            System.err.println(System.currentTimeMillis() + " " + getsResult.getStatus() + ": " + getsResult.getValue());
            // cas
            System.err.println(System.currentTimeMillis() + " cas... " + data);
            ClientRequest casRequest = client.cas(data, getsResult.getCas(), 30000);
            System.err.println(System.currentTimeMillis());
            ClientResult casResult = client.waitForCompletion(casRequest, 15000);
            System.err.println(System.currentTimeMillis() + " " + getsResult.getStatus() + ": " + casResult.getValue());
            // getAndLock
            System.err.println(System.currentTimeMillis() + " GetAndLock... " + data);
            ClientRequest getAndLockRequest = client.getAndLock(data, 30);
            System.err.println(System.currentTimeMillis());
            ClientResult getAndLockResult = client.waitForCompletion(getAndLockRequest, 15000);
            System.err.println(System.currentTimeMillis() + " " + getsResult.getStatus() + ": " + getAndLockResult.getValue());
            // unlock
            System.err.println(System.currentTimeMillis() + " GetAndLock... " + data);
            ClientRequest unlockRequest = client.unlock(data, getAndLockResult.getCas());
            System.err.println(System.currentTimeMillis());
            ClientResult unlockResult = client.waitForCompletion(unlockRequest, 15000);
            System.err.println(System.currentTimeMillis() + " " + getsResult.getStatus() + ": " + unlockResult.getValue());
            // delete
            System.err.println(System.currentTimeMillis() + " Delete... " + data);
            ClientRequest deleteRequest = client.delete(data);
            client.execOnCompletion(
                    deleteRequest,
                    15000,
                    new ExecOnCompletion() {
                        @Override
                        public void execute(ClientResult result) {
                            System.err.println(System.currentTimeMillis() + " Deleted1.. " + result.getStatus());
                        }
                    });
        } finally {
            Thread.sleep(2000);
            if (client != null) {
                client.shutdown();
            }
        }
    }
    
}
