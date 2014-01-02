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
package es.rickyepoderi.managertest.testng;

import es.rickyepoderi.managertest.client.Tester;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * <p>TestNG class for testing through the web services tester.</p>
 * 
 * @author ricky
 */
public class WebServicesTest {

    private final Tester test;

    @Parameters({ "baseUrl" })
    public WebServicesTest(String baseUrl) {
        test = new Tester();
        test.setBaseUrl(baseUrl);
        //test.setDebug(true);
    }

    @Test(groups = "blocking")
    public void test01BlockingWithSleep() throws Exception {
        System.out.println("** test01BlockingWithSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(2);
        test.setIterations(1);
        test.setChildIterations(1000);
        test.setThreadSleep(100);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }

    @Test(groups = "blocking")
    public void test02BlockingWithoutSleep() throws Exception {
        System.out.println("** test02BlockingWithoutSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(2);
        test.setIterations(1);
        test.setChildIterations(1000);
        test.setThreadSleep(0);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }

    @Test(groups = "refresh-update")
    public void test03RefreshUpdateWithSleep() throws Exception {
        System.out.println("** test03RefreshUpdateWithSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(1);
        test.setIterations(1);
        test.setChildIterations(1000);
        test.setThreadSleep(100);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }

    @Test(groups = "refresh-update")
    public void test04RefreshUpdateWithoutSleep() throws Exception {
        System.out.println("** test04RefreshUpdateWithoutSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(1);
        test.setIterations(1);
        test.setChildIterations(1000);
        test.setThreadSleep(0);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }

    @Test(groups = "create-delete")
    public void test05CreateDeleteWithSleep() throws Exception {
        System.out.println("** test05CreateDeleteWithSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(1);
        test.setIterations(500);
        test.setChildIterations(4);
        test.setThreadSleep(100);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }

    @Test(groups = "create-delete")
    public void test06CreateDeleteWithoutSleep() throws Exception {
        System.out.println("** test06CreateDeleteWithoutSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(1);
        test.setIterations(500);
        test.setChildIterations(4);
        test.setThreadSleep(0);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }
    
    @Test(groups = "external")
    public void test07ExternalWithSleep() throws Exception {
        System.out.println("** test07ExternalWithSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(1);
        test.setIterations(50);
        test.setChildIterations(50);
        test.setThreadSleep(100);
        test.setSizeAttr(12000);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }

    @Test(groups = "external")
    public void test08ExternalWithoutSleep() throws Exception {
        System.out.println("** test08ExternalWithoutSleep **");
        test.setNumThreads(1);
        test.setNumChildThreads(1);
        test.setIterations(50);
        test.setChildIterations(50);
        test.setThreadSleep(0);
        test.setSizeAttr(12000);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
    }

    /*
    @Test(groups = "performance")
    public void test21Performance1() throws Exception {
        System.out.println("** test21Performance1 **");
        test.setNumThreads(16);
        test.setNumChildThreads(1);
        test.setIterations(20);
        test.setChildIterations(50);
        test.setThreadSleep(500);
        test.setNumAttrs(1);
        test.setSizeAttr(50);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
        Thread.sleep(150000);
    }*/

    @Test(groups = "performance")
    public void test22Performance2() throws Exception {
        System.out.println("** test22Performance2 **");
        test.setNumThreads(16);
        test.setNumChildThreads(1);
        test.setIterations(20);
        test.setChildIterations(50);
        test.setThreadSleep(500);
        test.setNumAttrs(4);
        test.setSizeAttr(50);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
        Thread.sleep(150000);
    }

    /*
    @Test(groups = "performance")
    public void test23Performance3() throws Exception {
        System.out.println("** test23Performance3 **");
        test.setNumThreads(16);
        test.setNumChildThreads(1);
        test.setIterations(20);
        test.setChildIterations(50);
        test.setThreadSleep(500);
        test.setNumAttrs(20);
        test.setSizeAttr(100);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
        Thread.sleep(150000);
    }*/

    @Test(groups = "performance")
    public void test24Performance4() throws Exception {
        System.out.println("** test24Performance4 **");
        test.setNumThreads(16);
        test.setNumChildThreads(1);
        test.setIterations(20);
        test.setChildIterations(50);
        test.setThreadSleep(500);
        test.setNumAttrs(20);
        test.setSizeAttr(200);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
        Thread.sleep(150000);
    }
    
    @Test(groups = "performance")
    public void test25Performance5() throws Exception {
        System.out.println("** test25Performance5 **");
        test.setNumThreads(16);
        test.setNumChildThreads(1);
        test.setIterations(20);
        test.setChildIterations(50);
        test.setThreadSleep(500);
        test.setNumAttrs(10);
        test.setSizeAttr(12000);
        test.setUpdateAttributes(1);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
        Thread.sleep(150000);
    }
    
    @Test(groups = "performance")
    public void test26Performance6() throws Exception {
        System.out.println("** test26Performance6 **");
        test.setNumThreads(16);
        test.setNumChildThreads(1);
        test.setIterations(20);
        test.setChildIterations(50);
        test.setThreadSleep(500);
        test.setNumAttrs(10);
        test.setSizeAttr(12000);
        test.setUpdateAttributes(10);
        test.test();
        test.printResults();
        Assert.assertEquals(test.getTotalErrors(), 0L);
        Thread.sleep(150000);
    }
}
