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

import java.io.Serializable;
import java.util.UUID;

/**
 * <p>A reference object is the representation inside the internal session
 * map of a external attribute. It just contains the reference ot object
 * key in couchbase (UUID created), and three transient properties: the real 
 * value, a boolean that marks if it is modified and the last time touched or 
 * saved.</p>
 * 
 * @author ricky
 */
public class ReferenceObject implements Serializable {
    
    /**
     * The refrence to the couchbase object.
     */
    private String reference = null;
    
    /**
     * The real value.
     */
    transient private Object value = null;
    
    /**
     * Empty constructor.
     */
    public ReferenceObject() {
        this.reference = UUID.randomUUID().toString();
        this.value = null;
    }
    
    /**
     * Constructor vua reference.
     * @param reference The reference of the object.
     */
    public ReferenceObject(String reference) {
        this.reference = reference;
        this.value = null;
    }
    
    /**
     * Constructor via reference and value.
     * @param reference The reference of the object
     * @param value The value of the object
     */
    public ReferenceObject(String reference, Object value) {
        this.reference = reference;
        this.value = value;
    }
    
    /**
     * Getter for the reference.
     * @return The reference to the couchbase (the object key in couchbase)
     */
    public String getReference() {
        return reference;
    }

    /**
     * Setter for the reference.
     * @param reference The new reference
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Getter for the value,
     * @return The value of the object
     */
    public Object getValue() {
        return value;
    }

    /**
     * Setter for the value
     * @param value The new value of the object
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
