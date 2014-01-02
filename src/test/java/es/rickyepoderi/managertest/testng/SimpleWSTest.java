/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package es.rickyepoderi.managertest.testng;

import es.rickyepoderi.managertest.client.SessionTest;
import es.rickyepoderi.managertest.client.SessionTest_Service;
import es.rickyepoderi.managertest.client.Tester;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import junit.framework.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 *
 * @author ricky
 */
public class SimpleWSTest {
    
    private SessionTest test = null;
    
    @Parameters({ "baseUrl" })
    public SimpleWSTest(String baseUrl) throws Exception {
        // create a proxy for test with session
        SessionTest_Service service = new SessionTest_Service(
                    new URL(baseUrl),
                    new QName(Tester.DEFAULT_NAMESPACE, Tester.DEFAULT_LOCAL_PART));
        this.test = service.getSessionTestPort();
        ((BindingProvider)this.test).getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
    }
    
    private void check(String result) {
        Assert.assertFalse(result.startsWith("ERROR:"));
    }
    
    @Test(groups = "simplews")
    public void test01InternalAttribute() throws Exception {
        System.out.println("** test01InternalAttribute **");
        // create 3 attrs of 200 bytes set to 0
        check(test.createSession(3, 200, (byte) 0, 0));
        // check the three arrays are ok with 0
        check(test.checkSession(0, (byte) 0, 0));
        check(test.checkSession(1, (byte) 0, 0));
        check(test.checkSession(2, (byte) 0, 0));
        // update and check the 1
        check(test.updateSession(1, 200, (byte) 1, 0));
        check(test.checkSession(1, (byte) 1, 0));
        // delete 2 and check null
        check(test.deleteSessionAttribute(2, 0));
        check(test.checkNullSession(2, 0));
        // add 2 and check the value
        check(test.addSessionAttribute(2, 200, (byte) 2, 0));
        check(test.checkSession(2, (byte) 2, 0));
        // delete the session
        check(test.deleteSession(0));
    }
    
    @Test(groups = "simplews")
    public void test11ExternalAttribute() throws Exception {
        System.out.println("** test11ExternalAttribute **");
        // create 3 attrs of 12000 bytes set to 0
        check(test.createSession(4, 12000, (byte) 0, 0));
        // check the three arrays are ok with 0
        check(test.checkSession(0, (byte) 0, 0));
        check(test.checkSession(1, (byte) 0, 0));
        for (int i = 0; i < 100; i++) {
            // force the attrs 0 and 1 to be external (not used)
            check(test.checkSession(2, (byte) 0, 0));
            check(test.checkSession(3, (byte) 0, 0));
        }
        // update and check the 1 (it is external)
        check(test.updateSession(1, 12000, (byte) 1, 0));
        check(test.checkSession(1, (byte) 1, 0));
        // delete 0 and check null (it is external)
        check(test.deleteSessionAttribute(0, 0));
        check(test.checkNullSession(0, 0));
        // add 0 and check the value
        check(test.addSessionAttribute(0, 12000, (byte) 2, 0));
        check(test.checkSession(0, (byte) 2, 0));
        // delete the session
        check(test.deleteSession(0));
    }
    
    @Test(groups = "simplews")
    public void test21InternalToExternal() throws Exception {
        System.out.println("** test21InternalToExternal **");
        // create 3 attrs of 200 bytes set to 0
        check(test.createSession(3, 200, (byte) 0, 0));
        // check the three arrays are ok with 0
        check(test.checkSession(0, (byte) 0, 0));
        check(test.checkSession(1, (byte) 0, 0));
        check(test.checkSession(2, (byte) 0, 0));
        // update 1 to make it big enough to be external if not used
        check(test.updateSession(1, 12000, (byte) 1, 0));
        check(test.checkSession(1, (byte) 1, 0));
        // make external not reading it (reading 0)
        for (int i = 0; i < 100; i++) {
            check(test.checkSession(0, (byte) 0, 0));
        }
        // read it as external
        check(test.checkSession(1, (byte) 1, 0));
        // delete 1 to 200 to make it internal again
        check(test.updateSession(1, 200, (byte) 2, 0));
        check(test.checkSession(1, (byte) 2, 0));
        // finally delete 1
        check(test.deleteSessionAttribute(1, 0));
        check(test.checkNullSession(1, 0));
        // delete the session
        check(test.deleteSession(0));
    }
    
    @Test(groups = "simplews")
    public void test31Expiration() throws Exception {
        System.out.println("** test31Expiration **");
        // create 3 attrs of 200 bytes set to 0
        check(test.createSession(3, 200, (byte) 0, 0));
        // check the three arrays are ok with 0
        check(test.checkSession(0, (byte) 0, 0));
        check(test.checkSession(1, (byte) 0, 0));
        check(test.checkSession(2, (byte) 0, 0));
        // update 1 to make it big
        check(test.updateSession(1, 12000, (byte) 1, 0));
        check(test.checkSession(1, (byte) 1, 0));
        // create 3 big
        check(test.addSessionAttribute(3, 12000, (byte) 2, 0));
        check(test.checkSession(3, (byte) 2, 0));
        // read 0 to make 1 and 3 external
        for (int i = 0; i < 100; i++) {
            check(test.checkSession(0, (byte) 0, 0));
        }
        // sleep 12 minutes
        Thread.sleep(12*60*1000);
        // check all attrs are null
        check(test.checkNullSession(0, 0));
        check(test.checkNullSession(1, 0));
        check(test.checkNullSession(2, 0));
        check(test.checkNullSession(3, 0));
        // delete the new session
        check(test.deleteSession(0));
    }
}
