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
package es.rickyepoderi.couchbasemanager.session;

import es.rickyepoderi.couchbasemanager.couchbase.transcoders.TranscoderUtil;
import es.rickyepoderi.couchbasemanager.io.ReferenceObject;
import java.io.Serializable;

/**
 * <p>Class that manages some important information about an attribute. This 
 * information is used for several things:</p>
 * 
 * <ul>
 * <li>The serialized value of the attribute. When an attribute is read from
 * the couchbase it is not de-serialized and when the attribute is requested
 * it is converted into the real object.</li>
 * <li>There are some stats used to control when the attribute should be 
 * externalized. This tracking is done when the attribute is big (greater then 
 * the attrMaxSize). This attribute info is local to the session, not saved in 
 * couchbase. If there are several instances each instance has its numbers, that
 * is way the attribute is externalized using two values (lower and higher).</li>
 * </ul>
 * 
 * <p>The serialized attribute, when de-serialized is deleted. This way it
 * is controlled when this attribute was modified or it has been not accessed.
 * if it was not modified the same serialized value is used fro internal 
 * attributes. Non-sticky configuration deletes all values after saving but
 * sticky maintains both (serialized and de-serialized). Only for internal 
 * attributes, externalized ones are always removed (except the reference 
 * itself).</p>
 * 
 * @author ricky
 */
public class AttributeInfo implements Serializable {
    
    /**
     * Usage stats property
     */
    private UsageStats stats = null;
    
    /**
     * The real value of the attribute
     */
    private Object value = null;
    
    /**
     * The serialized object of the attribute
     */
    private byte[] serialized = null;
    
    /**
     * The object is a reference
     */
    private boolean isReference = false;
    
    /**
     * Empty constructor.
     */
    public AttributeInfo() {
        this.stats = null;
        this.value = null;
        this.serialized = null;
        this.isReference = false;
    }

    /**
     * Getter for the modified mark. An attribute is modified if the object has
     * been de-serialized.
     * @return the modified mark
     */
    public boolean isModified() {
        return serialized == null;
    }
    
    /**
     * It is de-serialized if the object is null.
     * @return if the object is de-serialized
     */
    public boolean isDeserialized() {
        return value != null;
    }

    /**
     * Return the value, if the object is a reference the PersistenceObject is
     * returned.
     * @return The value of the attribute info
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Return the real value, if it is a persistence object it returns the
     * value inside the reference.
     * @return The real value of the attribute
     */
    public Object getValueFrom() {
        if (isReference()) {
            return getReferenceObject();
        } else {
            return getValue();
        }
    }
    
    /**
     * Method to remove the reference. The value is set to null and is marked
     * as not a reference.
     * @param value 
     */
    public void removeReference(Object value) {
        this.value = value;
        this.serialized = null;
        this.isReference = false;
    }

    /**
     * Sets the real value for the attribute, if it is a reference the object
     * is set inside the reference.
     * @param value The new real value for the attribute
     */
    public void setValue(Object value) {
        // save the value as a reference or normal attribute
        if (isReference()) {
            ((ReferenceObject) this.value).setValue(value);
        } else {
            this.value = value;
        }
        // mark as modified and clean the old serialized array
        this.serialized = null;
    }

    /**
     * Getter for the serialized value.
     * @return The serialized value
     */
    public byte[] getSerialized() {
        return serialized;
    }

    /**
     * Setter for the serialized value. When the object is read from the 
     * byte array it is known if it is a reference or not. So this info
     * is also passed as a the second argument.
     * @param serialized The new serialized value
     * @param isReference If the value is a reference
     */
    public void setSerialized(byte[] serialized, boolean isReference) {
        this.serialized = serialized;
        this.isReference = isReference;
    }
    
    /**
     * Method that de-serializes the value from the serialize byte[] to the
     * real value.
     * @param trans  The transcoder to use
     */
    public void deserialize(TranscoderUtil trans) {
        if (isDeserialized()) {
            throw new IllegalStateException("The attribute is already de-serialized!");
        }
        this.value = trans.deserialize(this.serialized);
        this.serialized = null;
    }
    
    /**
     * Returns if the object is a reference. It can be known because it is 
     * marked (read from the serialized) or because the object is set.
     * @return true if it is a PersistenceObject
     */
    public boolean isReference() {
        return (value != null && value instanceof ReferenceObject) ||
                (value == null && isReference);
    }
    
    /**
     * Return the reference string of the PersistenceObject. If the object is
     * @return The reference of the externalized object
     */
    public String getReference() {
        if (!isReference()) {
            throw new IllegalStateException("The attribute is not a reference!");
        }
        return ((ReferenceObject)this.getValue()).getReference();
    }
    
    /**
     * It returns the ReferenceObject. It is assumed the object is a reference.
     * @return The ReferenceObject
     */
    public ReferenceObject getReferenceObject() {
        if (!isReference()) {
            throw new IllegalStateException("The attribute is not a reference!");
        }
        return (ReferenceObject) this.getValue();
    }
    
    /**
     * It returns the value of the ReferenceObject. It is assumed the object is
     * a ReferenceObject.
     * @return The value of the reference
     */
    public Object getReferenceValue() {
        if (!isReference()) {
            throw new IllegalStateException("The attribute is not a reference!");
        }
        return ((ReferenceObject)this.getValue()).getValue();
    }
    
    /**
     * It sets the value of the ReferenceObject.  It is assumed the object is
     * a ReferenceObject.
     * @param value The new value for the reference
     */
    public void setReferenceValue(Object value) {
        if (!isReference()) {
            throw new IllegalStateException("The attribute is not a reference!");
        }
        ((ReferenceObject)this.getValue()).setValue(value);
        // mark as modified and clean the old serialized array
        this.serialized = null;
    }
    
    /**
     * Method to know if the object is being tracked (stats are calculated to
     * check its externalization).
     * @return true if the stats are calculated
     */
    public boolean isStatsTracked() {
        return this.stats != null;
    }
    
    /**
     * Create the stats to start tracking of the attribute
     * @param startInfoTimes The times used if the attribute stats to be tracked 
     */
    public void createEmptyStats(long startInfoTimes) {
        if (this.stats == null) {
            this.stats = new UsageStats(startInfoTimes);
        }
    }
    
    /**
     * Method that increments the usage of the attribute. if it was not 
     * tracked, new stats are created and the attribute starts being tracked.
     * @param startInfoTimes The times used if the attribute stats to be tracked
     */
    public void incrementUsage(long startInfoTimes) {
        if (isStatsTracked()) {
            this.stats.incrementUsage();
        } else {
            this.stats = new UsageStats(startInfoTimes);
        }
    }
    
    /**
     * It returns the usage times of this attribute. It is assumed the attribute
     * is being tracked.
     * 
     * @param sessionUsageTimes The current session times
     * @return The times this attribute exists
     */
    public long getAttributeLiveTimes(long sessionUsageTimes) {
        return this.stats.getAttributeLiveTimes(sessionUsageTimes);
    }
    
    /**
     * The percentage of usage of this attribute. It depends on the times of
     * this attribute divided by the times of the session since this attribute
     * was created. It is assumed the attribute is being tracked.
     *
     * @param sessionUsageTimes The current session times
     * @return 0-100 percentage of use
     */
    public int getUsage(long sessionUsageTimes) {
        return this.stats.getUsage(sessionUsageTimes);
    }
    
    /**
     * The stats are cleaned and the attribute is not being tracked for now on.
     */
    public void cleanStats() {
        this.stats = null;
    }
    
    /**
     * Getter for the timestamp
     *
     * @return The last accessed timestamp
     */
    public long getLastTouch() {
        return this.stats.getLastTouch();
    }
    
    /**
     * Setter for the last accessed timestamp. Touches the attribute if tracked
     * (if it not assumed that the attribute is being tracked).
     *
     * @param lastTouch The new last accessed timestamp
     */
    public void setLastTouch(long lastTouch) {
        if (this.stats != null) {
            this.stats.setLastTouch(lastTouch);
        }
    }
    
    /**
     * String representation.
     * @return The string representation
     */
    @Override
    public String toString() {
        return new StringBuilder(this.getClass().getSimpleName())
                .append(" ")
                .append("serialized: ")
                .append(this.serialized != null)
                .append(" - value: ")
                .append(value)
                .append(" - isRef: ")
                .append(isReference())
                .toString();
    }
}
