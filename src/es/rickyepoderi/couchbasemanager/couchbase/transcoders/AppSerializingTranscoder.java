/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.couchbase.transcoders;

import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import java.io.*;
import net.spy.memcached.compat.CloseUtil;
import net.spy.memcached.transcoders.SerializingTranscoder;

/**
 *
 * <p>Simple transcoder that extends the normal spymemcached
 * SerializingTranscoder but the deserialize method is executed with
 * the AppObjectInputStream (in order to find application classes).</p>
 * 
 * @author ricky
 */
public class AppSerializingTranscoder extends SerializingTranscoder {

    /**
     * The class loader of the application
     */
    private ClassLoader appLoader = null;
    
    private JavaEEIOUtils ioUtils = null;
    
    /**
     * Empty Construstor.
     */
    public AppSerializingTranscoder() {
        super();
    }
    
    /**
     * Constructor using max size for byte array.
     * @param max Max size for the byte array.
     */
    public AppSerializingTranscoder(int max) {
        super(max);
    }
    
    /**
     * Constructor with the application class loader as an argument.
     * @param appLoader The application class loader
     * @param ioUtils The ObjectOutput/Input stream creator for Glassfish
     */
    public AppSerializingTranscoder(ClassLoader appLoader, JavaEEIOUtils ioUtils) {
        super();
        this.appLoader = appLoader;
        this.ioUtils = ioUtils;
    }

    /**
     * Constructor with the application class loader as an argument.
     * @param appLoader The application class loader
     * @param ioUtils The ObjectOutput/Input stream creator for Glassfish
     * @param max Limit for the buffer to serialize/deserialize
     */
    public AppSerializingTranscoder(ClassLoader appLoader, JavaEEIOUtils ioUtils, int max) {
        super(max);
        this.appLoader = appLoader;
        this.ioUtils = ioUtils;
    }
    
    //
    // GETTER & SETTERS
    //
    
    /**
     * Setter for the Application Class Loader.
     * @param appLoader The application class loader to use
     */
    public void setAppLoader(ClassLoader appLoader) {
        this.appLoader = appLoader;
    }
    
    /**
     * Setter for ioUtils to use Glassfish Object Input/Output Streams.
     * @param iOUtils Glassfish IOUtils.
     */
    public void setIoUtils(JavaEEIOUtils ioUtils) {
        this.ioUtils = ioUtils;
    }

    /**
     * Deserialize the object from the bytes read from couchbase to the object.
     * The only difference with normal SerializingTranscoder method is the
     * use of the new AppObjectInputStream to look for classes in both
     * class loaders (application and module).
     * @param in The read bytes
     * @return The object
     */
    @Override
    protected Object deserialize(byte[] in) {
        Object rv = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream is = null;
        try {
            if (in != null) {
                bis = new ByteArrayInputStream(in);
                if (appLoader != null) {
                    try {
                        is = ioUtils.createObjectInputStream(bis, true, appLoader);
                    } catch (Exception ex) {
                        getLogger().warn("Exception creating glassfish ObjectInputStream", ex);
                    }
                }
                if (is == null) {
                    is = new ObjectInputStream(bis);
                }
                rv = is.readObject();
                is.close();
                bis.close();
            }
        } catch (IOException e) {
            getLogger().warn("Caught IOException decoding %d bytes of data",
                    in == null ? 0 : in.length, e);
        } catch (ClassNotFoundException e) {
            getLogger().warn("Caught CNFE decoding %d bytes of data",
                    in == null ? 0 : in.length, e);
        } finally {
            CloseUtil.close(is);
            CloseUtil.close(bis);
        }
        return rv;
    }
   
    /**
     * Get the bytes representing the given serialized object.
     * @param o The object to serialize
     * @return The byte array representing the object
     */
    @Override
    protected byte[] serialize(Object o) {
        if (o == null) {
            throw new NullPointerException("Can't serialize null");
        }
        byte[] rv = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        try {
            bos = new ByteArrayOutputStream();
            try {
                os = ioUtils.createObjectOutputStream(new BufferedOutputStream(bos), true);
            } catch (Exception ex) {
                getLogger().warn("Exception creating glassfish ObjectOutputStream", ex);
            }
            if (os == null) {
                os = new ObjectOutputStream(new BufferedOutputStream(bos));
            }
            os.writeObject(o);
            os.close();
            bos.close();
            rv = bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            CloseUtil.close(os);
            CloseUtil.close(bos);
        }
        return rv;
    }
    
}
