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
 * <p>Class that maintains the stats of a tracked attribute. An attribute is 
 * tracked when its size is large enough (parameter <em>attrMaxSize</em>, by
 * default 10K.</p>
 * 
 * @author ricky
 */
public class UsageStats {

    /**
     * The time when the attribute was created in the session.
     */
    private long startInfoTimes = 0;

    /**
     * The number of times the attr has been got or put (modified).
     */
    private long attrTimes = 0;

    /**
     * timestamp for the last set / touch.
     */
    private long lastTouch = 0;

    /**
     * Constructor using the times the attribute is starting to be tracked.
     * @param startInfoTimes The times when the attribute is tracked
     */
    public UsageStats(long startInfoTimes) {
        this.startInfoTimes = startInfoTimes;
        this.attrTimes = 0;
        this.lastTouch = 0;
    }

    /**
     * The percentage of usage of this attribute. It depends on the times of
     * this attribute divided by the times of the session since this attribute
     * was created.
     *
     * @param sessionUsageTimes The current session times
     * @return 0-100 percentage of use
     */
    public int getUsage(long sessionUsageTimes) {
        // the usage of the attribute is the times used between the times
        // the session was used after the creation of the attribute
        // for example the attribute was created at usage 100 of the session
        // the attribute has been used 10 times and the session now has 120
        // the precentage of the attribute isage is 
        // (10 * 100) / (120 - 100) = 50%
        return (int) ((attrTimes * 100) / (sessionUsageTimes - startInfoTimes));
    }

    /**
     * The times this attribute was used.
     *
     * @return The number of times the attribute was used.
     */
    public long getAttributeUsageTimes() {
        return this.attrTimes;
    }

    /**
     * The times this attribute has been living. It is the subtraction of the
     * times when the attribute was created from the current session times.
     *
     * @param sessionUsageTimes The current session times
     * @return The times this attribute exists
     */
    public long getAttributeLiveTimes(long sessionUsageTimes) {
        return sessionUsageTimes - this.startInfoTimes;
    }

    /**
     * Increments the usage by one.
     */
    public void incrementUsage() {
        this.attrTimes++;
    }

    /**
     * Getter for the timestamp
     *
     * @return The last accessed timestamp
     */
    public long getLastTouch() {
        return lastTouch;
    }

    /**
     * Setter for the last accessed timestamp.
     *
     * @param lastTouch The new last accessed timestamp
     */
    public void setLastTouch(long lastTouch) {
        this.lastTouch = lastTouch;
    }
}
