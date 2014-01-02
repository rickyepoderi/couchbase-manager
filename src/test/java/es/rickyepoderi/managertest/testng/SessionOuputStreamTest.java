/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package es.rickyepoderi.managertest.testng;

import es.rickyepoderi.couchbasemanager.couchbase.transcoders.TranscoderUtil;
import es.rickyepoderi.couchbasemanager.io.SessionInputStream;
import es.rickyepoderi.couchbasemanager.io.SessionOutputStream;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author ricky
 */
public class SessionOuputStreamTest {
    
    @Test(groups = "io")
    public void test01() throws Exception {
        System.out.println("** test01 **");
        SessionOutputStream sos = null;
        SessionInputStream sis = null;
        try {
            sos = new SessionOutputStream();
            sos.writeInt(0);
            sos.writeLong(1L);
            sos.writeBoolean(true);
            sos.writeString("test01");
            sos.writeObject(new TranscoderUtil(), "sample1");
            sos.writeObject(new TranscoderUtil(), "sample2");
            sos.undo();
            sos.writeObject(new TranscoderUtil(), "sample3");
            sos.writeObject(new TranscoderUtil(), null);
            sos.flush();
            byte[] ouput = sos.toByteArray();
            sis = new SessionInputStream(ouput);
            Assert.assertEquals(sis.readInt(), 0);
            Assert.assertEquals(sis.readLong(), 1L);
            Assert.assertEquals(sis.readBoolean(), true);
            Assert.assertEquals(sis.readString(), "test01");
            Assert.assertEquals(sis.readObject(new TranscoderUtil()), "sample1");
            //Assert.assertEquals(sis.readObject(new TranscoderUtil()), "sample2");
            Assert.assertEquals(sis.readObject(new TranscoderUtil()), "sample3");
            Assert.assertNull(sis.readObject(new TranscoderUtil()));
        } finally {
            if (sos != null) {
                try {sos.close();} catch(IOException e) {}
            }
            if (sis != null) {
                try {sis.close();} catch(IOException e) {}
            }
        }
    }
}
