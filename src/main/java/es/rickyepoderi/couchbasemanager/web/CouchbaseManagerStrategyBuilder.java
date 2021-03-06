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
package es.rickyepoderi.couchbasemanager.web;

import com.sun.enterprise.web.BasePersistenceStrategyBuilder;
import es.rickyepoderi.couchbasemanager.session.UsageConfiguration;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;

/**
 *
 * <p>Class that builds the manager and establish it into the context of
 * an application. The documentation is <a href="http://docs.oracle.com/cd/E18930_01/html/821-2415/gkmhr.html">
 * here</a>. Since glassfish 4.0 this class is abstract and there are two
 * real extensions (for V3 and V4 respectively). This one just tries to 
 * group all the common things (v3 and v4 changed packages and how the
 * manager is inserted in the core).</p>
 * 
 * <p>Besides the service needs to be called "coherence-web", if other 
 * name is used it is not loaded (nasty checks inside glassfish code prevent
 * the manager to be actually loaded).See 
 * <a href="http://java.net/projects/glassfish/sources/svn/content/trunk/main/appserver/web/web-glue/src/main/java/com/sun/enterprise/web/SessionManagerConfigurationHelper.java">
 * SessionManagerConfigurationHelper</a> which performs some checks and
 * avoid using other custom names (even the CUSTOM).</p>
 * 
 * <p>The builder can be customized with some properties inside the 
 * the <em>glassfish-web.xml</em> deployment descriptor. The following properties
 * can be used right now:</p>
 * 
 * <ul>
 *   <li>repositoryUrl: The list of URIs of the couchbase servers, it is a
 *       comma separated list. For example "http://server1:8091/pools,http://server2:8091/pools".
 *       Default: "http://localhost:8091/pools".</li>
 *   <li>repositoryBucket: Repository bucket to use in couchbase. Default: "default".</li>
 *   <li>repositoryUsername: Repository admin username to use in the bucket. Default no user.</li>
 *   <li>repositoryPassword: Repository admin password. Default no password.</li>
 *   <li>sticky: Change the manager to be sticky. Sticky works very different,
 *       session is not locked in couchbase and it is not reload every request.
 *       Default false.</li>
 *   <li>lockTime: The amount of time in seconds that a key will be locked 
 *       inside couchbase manager without being released. In current couchbase 
 *       implementation the maximum locktime is 30 seconds. Default 30.</li>
 *   <li>operationTimeout: The time in milliseconds to wait for any operation
 *       against couchbase to timeout. Default 30000ms (30s).</li>
 *   <li>persistTo: the amount of nodes the item should be persisted to before 
 *       returning. This is a couchbase parameter to all write/modification
 *       operation, the value should be the String representation of the
 *       net.spy.memcached.PersistTo enum. Default ZERO.</li>
 *   <li>replicateTo: the amount of nodes the item should be replicated to 
 *       before returning. This is a couchbase parameter to all write/modification
 *       operation, the value should be the String representation of the
 *       net.spy.memcached.ReplicatedTo enum. Default ZERO.</li>
 *   <li>attrMaxSize: the maximum length of an internal attribute, if the
 *       attribute is longer than this size it is tracked to check if it should
 *       be externalized (written into another couchbase object). Specified in 
 *       bytes. Default: 10240 (10K).</li>
 *   <li>attrUsageCondition: This attribute contains three numbers: the minimum 
 *       data to start taking into account the usage information of the attribute;
 *       the lower percentage limit (a integrated value is externalized when the
 *       usage is below the lower limit); and the upper percentage limit (an 
 *       external attribute is reintegrated as internal if the usage goes above 
 *       this limit). Default: in sticky 10-10-25, in non-sticky 10-30-45.
 *   <li>attrTouchExtraTime: The external attributes are saved with an extra
 *       time (expiration), this way the external attributes are not always 
 *       touched when not used. Specified in seconds. Default: 600 (10 min).</li>
 *   <li>extraInactiveInterval: Since version 0.5.0 the session maximum
 *       inactivity interval is incremented by a extra amount of seconds in
 *       which the session remains in couchbase. This extra time is given to access
 *       session information before expiration in couchbase (JavaEE listeners
 *       mainly). Desfaul: 180 seconds.</li>
 * </ul>
 * 
 * <p>Example of configuration:</p>
 * 
 * <pre>
 * &lt;session-manager persistence-type="coherence-web"&gt;
 *   &lt;manager-properties&gt;
 *     &lt;property name="reapIntervalSeconds" value="20"/&gt;
 *     &lt;property name="repositoryUrl" value="http://localhost:8091/pools"/&gt;
 *   &lt;/manager-properties&gt;
 * &lt;/session-manager&gt;
 * </pre>
 * 
 * @author ricky
 */
public abstract class CouchbaseManagerStrategyBuilder extends BasePersistenceStrategyBuilder {

    //
    // PROPERTY NAMES
    //
    
    /**
     * Property to set the list (comma separated) of URIs to couchbase.
     */
    public static final String PROP_REPOSITORY_URL = "repositoryUrl";
    
    /**
     * Property to set the bucket to use.
     */
    public static final String PROP_REPOSITORY_BUCKET = "repositoryBucket";
    
    /**
     * Property to set the name of the user in couchbase.
     */
    public static final String PROP_REPOSITORY_USERNAME = "repositoryUsername";
    
    /**
     * Property to set the password of the user.
     */
    public static final String PROP_REPOSITORY_PASSWORD = "repositoryPassword";
    
    /**
     * Property to set the stickyness of the setup.
     */
    public static final String PROP_STICKY = "sticky";
    
    /**
     * Property that handles the amount of time in seconds to lock a key
     * inside getAndLock method (couchbase time has to be less or equal to 30s).
     */
    public static final String PROP_LOCK_TIME = "lockTime";
    
    /**
     * Property that manages the timeout for any couchbase operation. This 
     * timeout is managed in milliseconds.
     */
    public static final String PROP_OPERATION_TIMEOUT = "operationTimeout";
    
    /**
     * Property that manages the number of nodes to persist an operation.
     */
    public static final String PROP_PERSIST_TO = "persistTo";
    
    /**
     * Property that manages the number of nodes to replicate an operation.
     */
    public static final String PROP_REPLICATE_TO = "replicateTo";
    
    /**
     * Property that specify the maximum length to start tracking of the attribute.
     */
    public static final String PROP_ATTR_MAX_SIZE = "attrMaxSize";
    
    /**
     * Property to specify the extra time for external attributes (expiration).
     */
    public static final String PROP_ATTR_TOUCH_EXTRA_TIME = "attrTouchExtraTime";
    
    /**
     * Property that defined the usage condition to externalized attributes:
     * [c=0..N]-[l=0-100]-[h=0-100]. The "c" is the minimum count of accesses
     * used to calculate the usage percentage, "l" is the low limit when an
     * internal attribute is externalized for a low usage, "h" is the high limit
     * when an external attribute is re-integrated as internal. For example:
     * "20-20-30". All values should be integers.
     */
    public static final String PROP_ATTR_USAGE_CONDITION = "attrUsageCondition";
    
    /**
     * The property to define the extra number of seconds of expirations for
     * sessions in couchbase. Now the sessions are expired manually in order
     * to listeners to operate normally.
     */
    public static final String PROP_EXTRA_INACTIVE_INTERVAL = "extraInactiveInterval";
    
    //
    // DEFAULT VALUES FOR PROPERTIES
    //
    
    /**
     * Default value for repositoryUrl property.
     */
    protected static final String DEFAULT_REPOSITORY_URL = "http://localhost:8091/pools";
    
    /**
     * Default value for repositoryBucket property.
     */
    protected static final String DEFAULT_REPOSITORY_BUCKET = "default";
   
    /**
     * Default value for repositoryUsername property.
     */
    protected static final String DEFAULT_REPOSITORY_USERNAME = null;
    
    /**
     * Default value for repositoryPassword property.
     */
    protected static final String DEFAULT_REPOSITORY_PASSWORD = "";
    
    /**
     * Default value for stickyness (false).
     */
    protected static final boolean DEFAULT_STICKY = false;
    
    /**
     * Default lock time (maximum in couchbase 30s).
     */
    protected static final int DEFAULT_LOCK_TIME = 30;
    
    /**
     * Default operation timeout (30000ms = 30s).
     */
    protected static final long DEFAULT_OPERATION_TIMEOUT = 30000;
    
    /**
     * Default value for persistTo value (ZERO)
     */
    protected static final PersistTo DEFAULT_PERSIST_TO = PersistTo.ZERO;
    
    /**
     * Default value for replicateTo (ZERO)
     */
    protected static final ReplicateTo DEFAULT_REPLICATE_TO = ReplicateTo.ZERO;
    
    /**
     * Default maximum length (10K)
     */
    protected static final int DEFAULT_ATTR_MAX_SIZE = 10*1024;
    
    /**
     * Default extra time for external attributes (10 min)
     */
    protected static final int DEFAULT_ATTR_TOUCH_EXTRA_TIME = 10*60;
    
    /**
     * Default usage configuration for sticky: 10-10-25.
     */
    protected static final UsageConfiguration DEFAULT_USAGE_CONFIGURATION_STICKY = 
            new UsageConfiguration(10, 10, 25);
    
    /**
     * Default usage configuration for non-sticky: 10-30-45.
     */
    protected static final UsageConfiguration DEFAULT_USAGE_CONFIGURATION_NON_STICKY = 
            new UsageConfiguration(10, 30, 45);
    
    /**
     * The default number of seconds of extra life for sessions.
     */
    protected static final int DEFAULT_EXTRA_INACTIVE_INTERVAL = 3*60;
    
    //
    // REAL PROPERTIES
    //
    
    /**
     * property for URL.
     */
    protected String repositoryUrl = DEFAULT_REPOSITORY_URL;
    
    /**
     * property for bucket name.
     */
    protected String repositoryBucket = DEFAULT_REPOSITORY_BUCKET;
    
    /**
     * property for username.
     */
    protected String repositoryUsername = DEFAULT_REPOSITORY_USERNAME;
    
    /**
     * property for password.
     */
    protected String repositoryPassword = DEFAULT_REPOSITORY_PASSWORD;
    
    /**
     * property for stickyness.
     */
    protected boolean sticky = DEFAULT_STICKY;
    
    /**
     * property to manage the lock time.
     */
    protected int lockTime = DEFAULT_LOCK_TIME;
    
    /**
     * property to control the operation timeout in couchbase calls.
     */
    protected long operationTimeout = DEFAULT_OPERATION_TIMEOUT;
    
    /**
     * property to control the number of nodes to persist an operation.
     */
    protected PersistTo persistTo = DEFAULT_PERSIST_TO;
    
    /**
     * property to control the number of nodes to replicate an operation.
     */
    protected ReplicateTo replicateTo = DEFAULT_REPLICATE_TO;
    
    /**
     * property to specify the maximum length for internal attributes.
     */
    protected int attrMaxSize = DEFAULT_ATTR_MAX_SIZE;
    
    /**
     * property that manages the extra time for external attributes.
     */
    protected int attrTouchExtraTime = DEFAULT_ATTR_TOUCH_EXTRA_TIME;
    
    /**
     * The property for usage management for external attributes.
     */
    protected UsageConfiguration attrUsageCondition = null;
    
    /**
     * The property to handle the extra time giving for expiration of sessions.
     */
    protected int extraInactiveInterval = DEFAULT_EXTRA_INACTIVE_INTERVAL;
}