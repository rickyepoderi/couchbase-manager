/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager;

import com.couchbase.client.CouchbaseClient;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.Future;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import net.spy.memcached.internal.OperationFuture;

/**
 *
 * @author ricky
 */
public class Test2 {

    static public void main(String[] args) throws Exception {
        CouchbaseClient client = null;
        try {
            URI base = new URI("http://localhost:8091/pools");
            ArrayList baseURIs = new ArrayList();
            baseURIs.add(base);
            client = new CouchbaseClient(baseURIs, "default", null, "");
            String key = "dd2982ff68406c937040cd0c509b";
            
//            System.err.println("deleteing the object");
//            System.err.println("delete: " + client.delete(key).get());
//
            System.err.println("Creating the object");
            OperationFuture<Boolean> addFuture = client.add(key, 300, "lala");
            System.err.println("Status add: " + addFuture.getStatus());
            
            System.err.println("Getting and locking the object");
            OperationFuture<CASValue<Object>> future = client.asyncGetAndLock(key, 30);
            System.err.println("Status getl: " + future.getStatus());
            if (!future.getStatus().isSuccess()) {
                throw new Exception("Error locking!!!!");
            }
            System.err.println("Lock is mine");
            System.err.println("Status getl: " + future.get());
            
//            System.err.println("Getting the object");
//            OperationFuture<CASValue<Object>> future2 = client.asyncGets(key);
//            System.err.println("Status gets: " + future2.getStatus());
//            if (!future2.getStatus().isSuccess()) {
//                throw new Exception("Error gets!!!!");
//            }
//
//            System.err.println("Unlocking");
//            OperationFuture<Boolean> future3 = client.asyncUnlock(key, future.get().getCas());
//            System.err.println("Status unl: " + future3.getStatus());
//            if (!future3.getStatus().isSuccess()) {
//                throw new Exception("Error unlocking!!!!");
//            }
//            
//            System.err.println("CAS set");
//            Future<CASResponse> future3 = client.asyncCAS(key, future.get().getCas(), 
//                    300, "new value", new DefaultConnectionFactory().getDefaultTranscoder());
//            System.err.println("Error CAS: " + future3.get());
//            if (!future3.getStatus().isSuccess()) {
//                throw new Exception("Error unlocking!!!!");
//            }
            
            System.err.println("delete CAS: " + future.get().getCas());
            OperationFuture<Boolean> future4 = client.delete(key, future.get().getCas());
            //OperationFuture<Boolean> future4 = client.delete(key);
            System.err.println("delete: " + future4.getStatus());
            if (!future4.getStatus().isSuccess()) {
                throw new Exception("Error deleting!!!");
            }
           
        } finally {
            if (client != null) {
                client.shutdown();
            }
        }
    }
}
