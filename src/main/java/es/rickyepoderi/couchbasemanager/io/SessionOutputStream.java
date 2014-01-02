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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * <p>OutputStream used to serialize a session. It has special methods to write
 * all the different elements of the session.</p>
 * 
 * @author ricky
 */
public class SessionOutputStream extends ByteArrayOutputStream {
    
    /**
     * Data input stream used to write common primitives.
     */
    DataOutputStream dos = null;
    
    /**
     * Mark to the previous size of the array.
     */
    int previous = -1;
    
    /**
     * Empty constructor.
     */
    public SessionOutputStream() {
        super();
        dos = new DataOutputStream(this);
        previous = -1;
    }
    
    /**
     * Write a string using the bytes in UTF-8 encoding. It writes the length
     * of the array and then the byte array itself.
     * @param s The string to write
     * @return The length writen to the byte array
     * @throws IOException Some error writing the string
     */
    public int writeString(String s) throws IOException {
        previous = this.size();
        if (s == null) {
            dos.writeInt(-1);
        } else {
            byte[] bytes = s.getBytes("UTF-8");
            dos.writeInt(bytes.length);
            if (bytes.length > 0) {
                dos.write(bytes);
            }
        }
        dos.flush();
        return this.size() - previous;
    }
    
    /**
     * Write an object using the transcoder passed as argument.
     * @param trans The transcoder to use for serializing
     * @param o The object to write
     * @return The lenth of the object serialized
     * @throws IOException Some error writing the object
     */
    public int writeObject(TranscoderUtil trans, Object o) throws IOException {
        previous = this.size();
        trans.serialize(o, dos);
        dos.flush();
        return this.size() - previous;
    }
    
    /**
     * Write the boolean to the byte array.
     * @param b The boolean to write
     * @return the length written to the byte array
     * @throws IOException Some error writing the boolean
     */
    public int writeBoolean(boolean b) throws IOException {
        previous = this.size();
        dos.writeBoolean(b);
        dos.flush();
        return this.size() - previous;
    }
    
    /**
     * Write the long to the byte array.
     * @param l The long to write
     * @return The length of the long in the byte array
     * @throws IOException Some error writing the long
     */
    public int writeLong(long l) throws IOException {
        previous = this.size();
        dos.writeLong(l);
        dos.flush();
        return this.size() - previous;
    }
    
    /**
     * Write the int into the byte array.
     * @param i The integer to write
     * @return The length of the int written to the array
     * @throws IOException Some error writing the integer
     */
    public int writeInt(int i) throws IOException {
        previous = this.size();
        dos.writeInt(i);
        dos.flush();
        return this.size() - previous;
    }
    
    /**
     * Method to undo the last write. The previous position is restored and the
     * byte array corresponding to the last write. Only one operation can be 
     * undone.
     * @return The byte array corresponding to the last write
     * @throws IOException Some error doing the undo
     */
    public synchronized byte[] undo() throws IOException {
        if (this.previous == -1) {
            throw new IOException("Only one operation can be undone.");
        }
        byte[] copy = Arrays.copyOfRange(this.buf, previous, this.count);
        this.count = this.previous;
        this.previous = -1;
        return copy;
    }
}
