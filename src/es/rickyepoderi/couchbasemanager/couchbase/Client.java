/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.couchbase;

import com.couchbase.client.CouchbaseClient;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.spy.memcached.DefaultConnectionFactory;
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
    
    private Transcoder<Object> transcoder = null;
    
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
        this.transcoder = new DefaultConnectionFactory().getDefaultTranscoder();
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
        client = new CouchbaseClient(baseURIs, bucket, username, password);
        this.transcoder = transcoder;
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
     * executed this method gives the synch way of execution.
     * @param request The request to execute
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @return The client result os the operation
     */
    public ClientResult waitForCompletion(ClientRequest request, long timeout) {
        return request.waitForCompletion(timeout);
    }
    
    /**
     * After one operation is created this method executes it asynchronously.
     * The exec parameter let us execute some code after the operation is
     * finished. This method launches the request using a thread and this thread
     * waits for the operation to finish and then executes the exec.
     * @param request The request to execute async
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @param exec The code to execute after the operation is performed (can be null)
     */
    public void execOnCompletion(ClientRequest request, long timeout, ExecOnCompletion exec) {
        request.execOnCompletion(timeout, exec);
    }
    
    /**
     * Method to create a getAndLock operation.
     * @param data The data to receive
     * @param timeout The timeout of the lock
     * @return The request of this operation
     */
    public ClientRequest getAndLock(Data data, int timeout) {
        return ClientRequest.createGetAndLockResult(client.asyncGetAndLock(data.getId(), timeout, transcoder));
    }
    
    /**
     * Method to create a gets operation.
     * @param data The data to receive
     * @return The request of this operation
     */
    public ClientRequest gets(Data data) {
        return ClientRequest.createGets(client.asyncGets(data.getId(), transcoder));
    }
    
    /**
     * Method to create a set operation.
     * @param data The data to set
     * @param time The new expiration time
     * @return The request of this operation
     */
    public ClientRequest set(Data data, int time) {
        return ClientRequest.createSet(client.set(data.getId(), time, data, transcoder));
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
                client.asyncCAS(data.getId(), cas, time, data,
                transcoder)); // TODO: set a transcoder via configuration
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
        return ClientRequest.createUnlock(client.asyncUnlock(data.getId(), cas, transcoder));
    }
    
    /**
     * Method to create a touch operation.
     * @param data The data to touch
     * @param time The new expiration time
     * @return The request of this operation
     */
    public ClientRequest touch(Data data, int time) {
        return ClientRequest.createTouch(client.touch(data.getId(), time, transcoder));
    }
    
    /**
     * Method to create an add operation.
     * @param data The data to add
     * @param time The new expiration time
     * @return The request of this operation
     */
    public ClientRequest add(Data data, int time) {
        return ClientRequest.createAdd(client.add(data.getId(), time, data, transcoder));
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
            System.err.println("Adding... " + data);
            ClientRequest addRequest = client.add(data, 30000);
            ClientResult addResult = addRequest.waitForCompletion(15000);
            System.err.println(addResult.getStatus());
            // get
            System.err.println("Gets... " + data);
            ClientRequest getsRequest = client.gets(data);
            ClientResult getsResult = getsRequest.waitForCompletion(15000);
            System.err.println(getsResult.getStatus() + ": " + getsResult.getValue());
            // delete
            System.err.println("Delete... " + data);
            ClientRequest deleteRequest = client.delete(data);
            deleteRequest.execOnCompletion(
                    15000,
                    new ExecOnCompletion() {
                        @Override
                        public void execute(ClientResult result) {
                            System.err.println("Deleted1.. " + result.getStatus());
                        }
                    });
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }
    
}
