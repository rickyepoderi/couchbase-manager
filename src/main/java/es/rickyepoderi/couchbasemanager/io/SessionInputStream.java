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
package es.rickyepoderi.couchbasemanager.io;

import es.rickyepoderi.couchbasemanager.couchbase.transcoders.TranscoderUtil;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * <p>InputStream used to de-serialize a session. It has special methods to read
 * all the different elements of the session.</p>
 * 
 * @author ricky
 */
public class SessionInputStream extends ByteArrayInputStream {
    
    /**
     * The data input stream used to write ints, longs, boolean and so on.
     */
    DataInputStream dis = null;
    
    /**
     * Constructor using the byte array.
     * @param buf The byte array that contains a session
     */
    public SessionInputStream(byte[] buf) {
        super(buf);
        dis = new DataInputStream(this);
    }
    
    /**
     * A string is read just reading the length and the bytes using UTF-8
     * charset.
     * @return The read string.
     * @throws IOException Some error reading the byte array
     */
    public String readString() throws IOException {
        int length = dis.readInt();
        if (length == -1) {
            return null;
        } else if (length == 0) {
            return "";
        } else {
            byte[] bytes = new byte[length]; 
            dis.read(bytes);
            return new String(bytes, "UTF-8");
        }
    }
    
    /**
     * It reads any object using the transcoder passed to deserialize the
     * object.
     * @param trans The transcoder used to deserialize the object
     * @return The object read from the byte array
     * @throws IOException Some error reading the object
     */
    public Object readObject(TranscoderUtil trans) throws IOException {
        Object o = trans.deserialize(dis);
        return o;
    }
    
    /**
     * Method to read a long as DataInputStream.
     * @return The read long
     * @throws IOException Some error reading the long
     */
    public long readLong() throws IOException {
        return dis.readLong();
    }
    
    /**
     * Method to read a boolean as DataInputStream.
     * @return The boolean read
     * @throws IOException Some error reading the boolean
     */
    public boolean readBoolean() throws IOException {
        return dis.readBoolean();
    }
    
    /**
     * Method to read an int as DSAtaInputStream.
     * @return The read int
     * @throws IOException Some error reading the int
     */
    public int readInt() throws IOException {
        return dis.readInt();
    }
}
