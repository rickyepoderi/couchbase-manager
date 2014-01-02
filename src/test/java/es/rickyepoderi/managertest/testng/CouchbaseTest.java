/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package es.rickyepoderi.managertest.testng;

import es.rickyepoderi.couchbasemanager.couchbase.BulkClientRequest;
import es.rickyepoderi.couchbasemanager.couchbase.Client;
import es.rickyepoderi.couchbasemanager.couchbase.ClientRequest;
import es.rickyepoderi.couchbasemanager.couchbase.ClientResult;
import es.rickyepoderi.couchbasemanager.couchbase.ExecOnCompletion;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author ricky
 */
public class CouchbaseTest {
    
    private Client client = null;
    
    private class AsserterExec implements ExecOnCompletion {
        final private boolean success;
        
        public AsserterExec(boolean success) {
            this.success = success;
        }
        
        @Override
        public void execute(ClientResult result) {
            Assert.assertEquals(this.success, result.isSuccess());
        }
    }
    
    public CouchbaseTest() throws Exception {
        URI uri = new URI("http://localhost:8091/pools");
        List<URI> baseUris = new ArrayList<URI>();
        baseUris.add(uri);
        client = new Client(baseUris, "default", null, "");
        client.setTimeout(30000L);
    }
    
    private void sleep(long millis) {
        try {Thread.sleep(millis);} catch(InterruptedException e) {}
    }
    
    
    
    @Test(groups = "couchbase")
    public void test01SyncAddSetDelete() {
        System.out.println("** test01SyncAddSetDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ClientResult res = client.addSync(id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // set the entry with a new value
        data = "value2";
        res = client.setSync(id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // delete the entry
        res = client.deleteSync(id);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    @Test(groups = "couchbase")
    public void test02SyncAddCasDelete() {
        System.out.println("** test02SyncAddCasDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ClientResult res = client.addSync(id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // cas the entry with a new value
        data = "value2";
        res = client.casSync(id, data.getBytes(), res.getCas(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // delete the entry
        res = client.deleteSync(id, res.getCas());
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    @Test(groups = "couchbase")
    public void test03SyncAddTouchExpire() {
        System.out.println("** test03SyncAddTouchExpire **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ClientResult res = client.addSync(id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // touch the entry
        res = client.touchSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        // sleep and find before expiration
        sleep(6000);
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // sleep and find after expiration
        sleep(6000);
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    @Test(groups = "couchbase")
    public void test04SyncAddLockCasLockDelete() {
        System.out.println("** test04SyncAddLockCasLockDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ClientResult res = client.addSync(id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        // get and lock
        res = client.getAndLockSync(id, 10);
        long cas = res.getCas();
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // set the value => error
        data = "value2";
        res = client.setSync(id, data.getBytes(), 10);
        Assert.assertFalse(res.isSuccess());
        // cas the entry with a new value => ok
        res = client.casSync(id, data.getBytes(), cas, 10);
        Assert.assertTrue(res.isSuccess());
        // get and lock
        res = client.getAndLockSync(id, 10);
        cas = res.getCas();
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // delete without cas => error
        res = client.deleteSync(id);
        Assert.assertFalse(res.isSuccess());
        // delete the entry with cas => ok
        res = client.deleteSync(id, cas);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    
    
    @Test(groups = "couchbase")
    public void test11AsyncAddSetDelete() {
        System.out.println("** test11AsyncAddSetDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ExecOnCompletion exec = new AsserterExec(true);
        ClientRequest req = client.addAsync(id, data.getBytes(), 10, exec);
        client.waitForCompletion(req);
        ClientResult res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // set the entry with a new value
        data = "value2";
        req = client.setAsync(id, data.getBytes(), 10, exec);
        client.waitForCompletion(req);
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // delete the entry
        req = client.deleteAsync(id, exec);
        client.waitForCompletion(req);
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    @Test(groups = "couchbase")
    public void test12AsyncAddCasDelete() {
        System.out.println("** test12AsyncAddCasDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ExecOnCompletion exec = new AsserterExec(true);
        ClientRequest req = client.addAsync(id, data.getBytes(), 10, exec);
        client.waitForCompletion(req);
        ClientResult res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // cas the entry with a new value
        data = "value2";
        req = client.casAsync(id, data.getBytes(), res.getCas(), 10, exec);
        client.waitForCompletion(req);
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // delete the entry
        req = client.deleteAsync(id, res.getCas(), exec);
        client.waitForCompletion(req);
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    @Test(groups = "couchbase")
    public void test13AsyncAddTouchExpire() {
        System.out.println("** test13AsyncAddTouchExpire **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ExecOnCompletion exec = new AsserterExec(true);
        ClientRequest req = client.addAsync(id, data.getBytes(), 10, exec);
        client.waitForCompletion(req);
        ClientResult res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // touch the entry
        req = client.touchAsync(id, 10, exec);
        client.waitForCompletion(req);
        // sleep and find before expiration
        sleep(6000);
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // sleep and find after expiration
        sleep(6000);
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    @Test(groups = "couchbase")
    public void test14AsyncAddLockCasLockDelete() {
        System.out.println("** test14AsyncAddLockCasLockDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        ExecOnCompletion execOk = new AsserterExec(true);
        ExecOnCompletion execError = new AsserterExec(false);
        ClientRequest req = client.addAsync(id, data.getBytes(), 10, execOk);
        client.waitForCompletion(req);
        // get and lock
        ClientResult res = client.getAndLockSync(id, 10);
        long cas = res.getCas();
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // set the value => error
        data = "value2";
        req = client.setAsync(id, data.getBytes(), 10, execError);
        client.waitForCompletion(req);
        // cas the entry with a new value => ok
        req = client.casAsync(id, data.getBytes(), cas, 10, execOk);
        client.waitForCompletion(req);
        // get and lock
        res = client.getAndLockSync(id, 10);
        client.waitForCompletion(req);
        cas = res.getCas();
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // delete without cas => error
        req = client.deleteAsync(id, execError);
        client.waitForCompletion(req);
        // delete the entry with cas => ok
        req = client.deleteAsync(id, cas, execOk);
        client.waitForCompletion(req);
        // check it is deleted
        res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
    }
    
    private long assertBulkValues(String id, String data, Map<String, String> aux) {
        ClientResult res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        long cas = res.getCas();
        Assert.assertEquals(new String(res.getValue()), data);
        for (Map.Entry<String, String> e: aux.entrySet()) {
            res = client.getsSync(e.getKey());
            Assert.assertEquals(new String(res.getValue()), e.getValue());
        }
        return cas;
    }
    
    private void assertBulkDeleted(String id, Map<String, String> aux) {
        ClientResult res = client.getsSync(id);
        Assert.assertTrue(res.isNotFound());
        for (Map.Entry<String, String> e: aux.entrySet()) {
            res = client.getsSync(e.getKey());
            Assert.assertTrue(res.isNotFound());
        }
    }
    
    
    
    @Test(groups = "couchbase")
    public void test21BulkSyncAddSetDelete() {
        System.out.println("** test21BulkSyncAddSetDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.finishAddSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.finishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.finishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        res = client.finishDeleteSync(bulk, id);
        Assert.assertTrue(res.isSuccess());
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test22BulkSyncAddCasDelete() {
        System.out.println("** test22BulkSyncAddCasDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.finishAddSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.finishCasSync(bulk, id, data.getBytes(), res.getCas(), 10);
        Assert.assertTrue(res.isSuccess());
        long cas = res.getCas();
        assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a cas
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.finishCasSync(bulk, id, data.getBytes(), cas, 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        res = client.finishDeleteSync(bulk, id);
        Assert.assertTrue(res.isSuccess());
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test23BulkSyncAddTouchExpire() {
        System.out.println("** test23BulkSyncAddTouchExpire **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.finishAddSync(bulk, id, data.getBytes(), 10);Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.finishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // create a bulk and touch all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationTouch(bulk, e.getKey(), 10);
        }
        res = client.finishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        // wait and check they exists
        sleep(6000);
        assertBulkValues(id, data, aux);
        // wait and check expired
        sleep(6000);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test24BulkSyncAddLockCasLockDelete() {
        System.out.println("** test24BulkSyncAddLockCasLockDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.finishAddSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.finishCasSync(bulk, id, data.getBytes(), res.getCas(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // lock and delete all
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        res = client.finishDeleteSync(bulk, id, res.getCas());
        Assert.assertTrue(res.isSuccess());
        assertBulkDeleted(id, aux);
    }
    
    
    
    @Test(groups = "couchbase")
    public void test31BulkWaitSyncAddSetDelete() {
        System.out.println("** test31BulkWaitSyncAddSetDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.waitAndFinishAddSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.waitAndFinishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.waitAndFinishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        res = client.waitAndFinishDeleteSync(bulk, id);
        Assert.assertTrue(res.isSuccess());
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test32BulkWaitSyncAddCasDelete() {
        System.out.println("** test32BulkWaitSyncAddCasDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.waitAndFinishAddSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.waitAndFinishCasSync(bulk, id, data.getBytes(), res.getCas(), 10);
        Assert.assertTrue(res.isSuccess());
        long cas = res.getCas();
        assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a cas
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.waitAndFinishCasSync(bulk, id, data.getBytes(), cas, 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        res = client.waitAndFinishDeleteSync(bulk, id);
        Assert.assertTrue(res.isSuccess());
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test33BulkWaitSyncAddTouchExpire() {
        System.out.println("** test33BulkWaitSyncAddTouchExpire **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.waitAndFinishAddSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.waitAndFinishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // create a bulk and touch all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationTouch(bulk, e.getKey(), 10);
        }
        res = client.waitAndFinishSetSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        // wait and check they exists
        sleep(6000);
        assertBulkValues(id, data, aux);
        // wait and check expired
        sleep(6000);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test34BulkWaitSyncAddLockCasLockDelete() {
        System.out.println("** test34BulkWaitSyncAddLockCasLockDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        ClientResult res = client.waitAndFinishAddSync(bulk, id, data.getBytes(), 10);
        Assert.assertTrue(res.isSuccess());
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        res = client.waitAndFinishCasSync(bulk, id, data.getBytes(), res.getCas(), 10);
        Assert.assertTrue(res.isSuccess());
        assertBulkValues(id, data, aux);
        // lock and delete all
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        res = client.waitAndFinishDeleteSync(bulk, id, res.getCas());
        Assert.assertTrue(res.isSuccess());
        assertBulkDeleted(id, aux);
    }
    
    
    @Test(groups = "couchbase")
    public void test41BulkAsyncAddSetDelete() {
        System.out.println("** test41BulkAsyncAddSetDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.finishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.finishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.finishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        client.finishDeleteAsync(bulk, id, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test42BulkAsyncAddCasDelete() {
        System.out.println("** test42BulkAsyncAddCasDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.finishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.finishCasAsync(bulk, id, data.getBytes(), res.getCas(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        long cas = assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a cas
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.finishCasAsync(bulk, id, data.getBytes(), cas, 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        client.finishDeleteAsync(bulk, id, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test43BulkAsyncAddTouchExpire() {
        System.out.println("** test43BulkAsyncAddTouchExpire **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.finishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.finishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // create a bulk and touch all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationTouch(bulk, e.getKey(), 10);
        }
        client.finishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        // wait and check they exists
        sleep(6000);
        assertBulkValues(id, data, aux);
        // wait and check expired
        sleep(6000);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test44BulkAsyncAddLockCasLockDelete() {
        System.out.println("** test44BulkAsyncAddLockCasLockDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.finishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.finishCasAsync(bulk, id, data.getBytes(), res.getCas(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // lock and delete all
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        client.finishDeleteAsync(bulk, id, res.getCas(), new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkDeleted(id, aux);
    }
    
    
    
    @Test(groups = "couchbase")
    public void test51BulkWaitAsyncAddSetDelete() {
        System.out.println("** test51BulkWaitAsyncAddSetDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.waitAndFinishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.waitAndFinishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.waitAndFinishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        client.waitAndFinishDeleteAsync(bulk, id, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test52BulkWaitAsyncAddCasDelete() {
        System.out.println("** test52BulkWaitAsyncAddCasDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.waitAndFinishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.waitAndFinishCasAsync(bulk, id, data.getBytes(), res.getCas(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        long cas = assertBulkValues(id, data, aux);
        // create a bulk and set all the aux values, finish with a cas
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            e.setValue(e.getValue() + "-2");
            client.addOperationSet(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.waitAndFinishCasAsync(bulk, id, data.getBytes(), cas, 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // delete the entry with another bulk
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        client.waitAndFinishDeleteAsync(bulk, id, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test53BulkWaitAsyncAddTouchExpire() {
        System.out.println("** test53BulkWaitAsyncAddTouchExpire **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.waitAndFinishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getsSync(id);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a set
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.waitAndFinishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // create a bulk and touch all the aux values, finish with a set
        data = "value3";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationTouch(bulk, e.getKey(), 10);
        }
        client.waitAndFinishSetAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        // wait and check they exists
        sleep(6000);
        assertBulkValues(id, data, aux);
        // wait and check expired
        sleep(6000);
        assertBulkDeleted(id, aux);
    }
    
    @Test(groups = "couchbase")
    public void test54BulkWaitAsyncAddLockCasLockDelete() {
        System.out.println("** test54BulkWaitAsyncAddLockCasLockDelete **");
        String id = UUID.randomUUID().toString();
        String data = "value1";
        // add a new entry
        BulkClientRequest bulk = client.createBulk();
        client.waitAndFinishAddAsync(bulk, id, data.getBytes(), 10, new AsserterExec(true));
        ClientResult res = client.waitForCompletion(bulk);
        Assert.assertTrue(res.isSuccess());
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        // add some aux keys
        Map<String, String> aux = new HashMap<String, String>();
        for (int i = 0; i < 4; i++) {
            aux.put(UUID.randomUUID().toString(), "aux" + i + "-1");
        }
        // create a bulk and add all the aux values, finish with a cas
        data = "value2";
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationAdd(bulk, e.getKey(), e.getValue().getBytes(), 10);
        }
        client.waitAndFinishCasAsync(bulk, id, data.getBytes(), res.getCas(), 10, new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkValues(id, data, aux);
        // lock and delete all
        res = client.getAndLockSync(id, 10);
        Assert.assertTrue(res.isSuccess());
        Assert.assertEquals(new String(res.getValue()), data);
        bulk = client.createBulk();
        for (Map.Entry<String, String> e: aux.entrySet()) {
            client.addOperationDelete(bulk, e.getKey());
        }
        client.waitAndFinishDeleteAsync(bulk, id, res.getCas(), new AsserterExec(true));
        client.waitForCompletion(bulk);
        assertBulkDeleted(id, aux);
    }
}
