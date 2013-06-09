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
package es.rickyepoderi.couchbasemanager.couchbase;

import com.couchbase.client.CouchbaseConnectionFactory;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import net.spy.memcached.transcoders.Transcoder;

/**
 *
 * <p>Simple couchbase factory to use a specific transcoder. The transcoder
 * is used all the time, so it should be support multi thread access.</p>
 * 
 * @author ricky
 */
public class AppCouchbaseConnectionFactory extends CouchbaseConnectionFactory {
    
    /**
     * The transcoder to use.
     */
    private Transcoder<Object> transcoder = null;
    
    /**
     * Constructor using the super parameters plus the transcoder.
     * @param baseList The list of couchbase servers
     * @param bucketName The bucket name to use
     * @param password The password to use
     * @param transcoder The transcoder to use
     * @throws IOException  Some error initializing the factory
     */
    public AppCouchbaseConnectionFactory(final List<URI> baseList,
      final String bucketName, String password, Transcoder transcoder) throws IOException {
        super(baseList, bucketName, password);
        this.transcoder = transcoder;
    }

    /**
     * Getter for the transcoder. It is overridden cos it is the only way 
     * of returning another transcoder to the default one.
     * @return The transcoder of the factory
     */
    @Override
    public Transcoder<Object> getDefaultTranscoder() {
        return transcoder;
    }
    
    
}
