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

/**
 * <p>Class to contain the three values of the usage: minimum count
 * of usages, low level when an internal attribute is externalized, and high 
 * level when an external attribute is re-integrated.</p>
 * 
 * @author ricky
 */
public class UsageConfiguration {
    
    /**
     * Minimum access of the session since attribute was created.
     */
    private int minimum = 0;
    
    /**
     * The low level when an internal attribute is externalized because a 
     * low level usage.
     */
    private int low = 0;
    
    /**
     * The high level when an external attribute is re-integrated cos
     * it is being used again.
     */
    private int high = 0;

    /**
     * Constructor via properties.
     * @param minimum The minimum count
     * @param low The low level
     * @param high The high level
     */
    public UsageConfiguration(int minimum, int low, int high) {
        this.minimum = minimum;
        this.low = low;
        this.high = high;
        if (low > high) {
            throw new NumberFormatException("The low limit should be lower than high limit");
        }
        if (minimum < 1) {
            throw new NumberFormatException("The minimum value is 1");
        }
    }
    
    /**
     * Constructor using the definition "[c=0..N]-[l=0-100]-[h=0-100]".
     * @param definition The definition of the property 
     */
    public UsageConfiguration(String definition) throws NumberFormatException {
        if (!definition.matches("[0-9]+-[0-9]+-[0-9]+")) {
            throw new NumberFormatException(
                String.format("Invalid definition: %s", definition));
        }
        String[] numbers = definition.split("-");
        this.minimum = Integer.parseInt(numbers[0]);
        this.low = Integer.parseInt(numbers[1]);
        this.high = Integer.parseInt(numbers[2]);
        if (low > high) {
            throw new NumberFormatException("The low limit should be lower than high limit");
        }
        if (minimum < 1) {
            throw new NumberFormatException("The minimum value is 1");
        }
    }
    
    /**
     * Getter for minimum.
     * @return Minimum access of the session since attribute was created.
     */
    public int getMinimum() {
        return minimum;
    }

    /**
     * Getter for the low limit.
     * @return The low level when an internal attribute is externalized because a 
     * low level usage.
     */
    public int getLow() {
        return low;
    }

    /**
     * Getter for the high limit.
     * @return The high level when an external attribute is re-integrated cos
     * it is being used again.
     */
    public int getHigh() {
        return high;
    }
}
