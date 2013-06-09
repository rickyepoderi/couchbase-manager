/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager;

import com.couchbase.client.CouchbaseClient;
import java.net.URI;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.CASValue;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;

/**
 *
 * @author ricky
 */
public class Test {
    
    static public class TesterThread extends Thread {

        private String key;
        private CouchbaseClient client;
        
        public TesterThread(CouchbaseClient client, String key) {
            this.key = key;
            this.client = client;
        }
        
        @Override
        public void run() {
            try {
                System.err.println(this + ": Getting and locking the object");
                CASValue cas = null;
                while (cas == null || cas.getValue() == null) {
                    cas = client.getAndLock(key, 30);
                    OperationFuture<CASValue<Object>> future = client.asyncGetAndLock(key, 30);
                    future.get(new DefaultConnectionFactory().getOperationTimeout(), 
                            TimeUnit.MILLISECONDS);
                    System.err.println("Error: " + future.getStatus());
                    Thread.sleep(1000);
                }
                System.err.println(this + ": Lock is mine");
                System.err.println(cas.getCas() + ":" + cas.getValue());
                
                Thread.sleep(2000);
                
                // CAS => set new value and unlock
                System.err.println(this + ": Setting new object");
                client.cas("key1", cas.getCas(), 30, hashCode(), new DefaultConnectionFactory().getDefaultTranscoder());
                
                // TOUCH and unlock => no modification
                System.err.println(this + ": unlocking");
                //System.err.println(client.touch(key, 300));
                OperationFuture<Boolean> future =client.asyncUnlock(key, cas.getCas());
                System.err.println("Error unlock: " + future.getStatus());
                
                System.err.println(this + ": object: " + client.get(key));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    static public void main(String[] args) throws Exception {
        URI base = new URI("http://localhost:8091/pools");
        ArrayList baseURIs = new ArrayList();
        baseURIs.add(base);
        CouchbaseClient client = new CouchbaseClient(baseURIs, "default", null, "");
        System.err.println("Creating the object");
        //client.set("key1", 300, "initial value");
        String key = "key1";
        
        Thread t1 = new TesterThread(client, key);
        //Thread t2 = new TesterThread(client, key);
        
        t1.start();
        //t2.start();
        
        t1.join();
        //t2.join();
        
        client.shutdown();
    }
}
