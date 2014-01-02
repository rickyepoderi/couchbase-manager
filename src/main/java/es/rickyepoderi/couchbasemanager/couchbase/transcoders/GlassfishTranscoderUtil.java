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
package es.rickyepoderi.couchbasemanager.couchbase.transcoders;

import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import es.rickyepoderi.couchbasemanager.io.NullObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.compat.CloseUtil;

/**
 *
 * @author ricky
 */
public class GlassfishTranscoderUtil extends TranscoderUtil {
    
    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(GlassfishTranscoderUtil.class.getName());
    
    /**
     * The application class loader of the container.
     */
    private ClassLoader appLoader = null;
    
    /**
     * The glassfish IOUtils used to create object input output streams.
     */
    private JavaEEIOUtils ioUtils = null;
    
    /**
     * Empty constructor.
     */
    public GlassfishTranscoderUtil() {
        super();
    }
    
    /**
     * Constructor via properties.
     * @param appLoader The application class loader
     * @param ioUtils The glasssfish io utils
     */
    public GlassfishTranscoderUtil(ClassLoader appLoader, JavaEEIOUtils ioUtils) {
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
     * @param ioUtils Glassfish IOUtils.
     */
    public void setIoUtils(JavaEEIOUtils ioUtils) {
        this.ioUtils = ioUtils;
    }
    
    /**
     * De-serialize a object from a input stream. In the glassfish 
     * transcoder the glassfish ioutils are used.
     * @param in The input stream for reading the object from
     * @return The object de-serialized
     */
    @Override
    public Object deserialize(InputStream in) {
        Object rv = null;
        ObjectInputStream is = null;
        try {
            is = ioUtils.createObjectInputStream(in, true, appLoader);
            rv = is.readObject();
            is.close();
            if (rv instanceof NullObject) {
                rv = null;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Caught Exception decoding bytes of data", e);
        } finally {
            CloseUtil.close(is);
        }
        return rv;
    }
    
    /**
     * Serialize an object in a object output stream. In the glassfish 
     * transcoder the glassfish ioutils are used.
     * @param o The object to serialize
     * @param out The output stream
     */
    @Override
    public void serialize(Object o, OutputStream out) {
        if (o == null) {
            o = new NullObject();
        }
        ObjectOutputStream os = null;
        try {
            os = ioUtils.createObjectOutputStream(out, true);
            os.writeObject(o);
            os.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Non-serializable object", e);
        } finally {
            CloseUtil.close(os);
        }
    }
}
