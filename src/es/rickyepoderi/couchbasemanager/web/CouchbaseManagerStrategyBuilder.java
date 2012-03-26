/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.rickyepoderi.couchbasemanager.web;

import com.sun.enterprise.deployment.runtime.web.ManagerProperties;
import com.sun.enterprise.deployment.runtime.web.SessionManager;
import com.sun.enterprise.deployment.runtime.web.WebProperty;
import com.sun.enterprise.web.BasePersistenceStrategyBuilder;
import com.sun.enterprise.web.ServerConfigLookup;
import es.rickyepoderi.couchbasemanager.session.CouchbaseManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * <p>Class that builds the manager and establish it into the context of
 * an application. The documentation is <a href="http://docs.oracle.com/cd/E18930_01/html/821-2415/gkmhr.html">
 * here</a>. Besides the manager has to be defined inside in 
 * META-INF/inhabitants/default as the manager with the next line:</p>
 * 
 * <pre>
 * class=com.sun.enterprise.web.MemoryStrategyBuilder,index=com.sun.enterprise.web.PersistenceStrategyBuilder:memory
 * </pre>
 * 
 * <p>Besides the service needs to be called "coherence-web", if other 
 * name is used it is not loaded (nasty checks inside glassfish code prevent
 * the manager to be actually loaded).See 
 * <a href="http://java.net/projects/glassfish/sources/svn/content/trunk/main/appserver/web/web-glue/src/main/java/com/sun/enterprise/web/SessionManagerConfigurationHelper.java">
 * SessionManagerConfigurationHelper</a> which performs some checks and
 * avoid using other custom names (even the CUSTOM).</p>
 * 
 * <p>The builder can be customized with some properties inside the 
 * the glassfish-web.xml deployment descriptor. The following properties
 * can be used right now:</p>
 * 
 * <ul>
 *   <li>repositoryUrl: The list of URIs of the couchbase servers, it is
 *       comma separated list. For example "http://server1:8091/pools,http://server2:8091/pools".
 *       Default: "http://localhost:8091/pools".</li>
 *   <li>repositoryBucket: Repository bucket to use in couchbase. Default: "default".</li>
 *   <li>repositoryUsername: Repository admin username to use in the bucket. Default no user.</li>
 *   <li>repositoryPassword: Repository admin password. Default no password.</li>
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
@Service(name = "coherence-web")
public class CouchbaseManagerStrategyBuilder extends BasePersistenceStrategyBuilder {

    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(CouchbaseManagerStrategyBuilder.class.getName());
    
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
     * Main method that creates the manager and sets it to the context of the 
     * application. The properties are read from the glassfish-web.xml and
     * the manager is created and assign to the context.
     * 
     * @param ctx The context to assign the manager
     * @param smBean The bean where properties are read
     * @param serverConfigLookup Configuration lookup
     */
    @Override
    public void initializePersistenceStrategy(Context ctx,
            SessionManager smBean, ServerConfigLookup serverConfigLookup) {
        log.fine("MemManagerStrategyBuilder.initializePersistenceStrategy: init");
        super.initializePersistenceStrategy(ctx, smBean, serverConfigLookup);
        // create the memory manager
        CouchbaseManager manager = new CouchbaseManager(repositoryUrl);
        // set values read from the configuration
        manager.setBucket(repositoryBucket);
        manager.setUsername(repositoryUsername);
        manager.setPassword(repositoryPassword);
        // TODO: set more values
        StandardContext sctx = (StandardContext) ctx;
        if (!sctx.isSessionTimeoutOveridden()) {
            log.log(Level.FINE,
                    "MemManagerStrategyBuilder.initializePersistenceStrategy: sessionMaxInactiveInterval {0}",
                    this.sessionMaxInactiveInterval);
            manager.setMaxInactiveInterval(this.sessionMaxInactiveInterval);
        }
        ctx.setManager(manager);
        // add the mem manager valve
        //log.fine("MemManagerStrategyBuilder.initializePersistenceStrategy: adding MemManagerValve");
        //MemManagerValve memValve = new MemManagerValve();
        //sctx.addValve((GlassFishValve)memValve);
        //log.fine("MemManagerStrategyBuilder.initializePersistenceStrategy: exit");
    }
    
    /**
     * Overriden method to read extra parameter for couchbase manager.
     * The method call super and then re-parse the configuration to
     * assign local properties.
     * 
     * @param ctx The context
     * @param smBean The bean
     */
    @Override
    public void readWebAppParams(Context ctx, SessionManager smBean ) { 
        log.fine("MemManagerStrategyBuilder.readWebAppParams: init");
        // read normal properties
        super.readWebAppParams(ctx, smBean);
        // TODO: add more parameters to read from the config
        //       This method is in BasePersistenceStrategyBuilder
        if (smBean != null) {
            // read extra parameters
            ManagerProperties mgrBean = smBean.getManagerProperties();
            if ((mgrBean != null) && (mgrBean.sizeWebProperty() > 0)) {
                for (WebProperty prop : mgrBean.getWebProperty()) {
                    String name = prop.getAttributeValue(WebProperty.NAME);
                    String value = prop.getAttributeValue(WebProperty.VALUE);
                    if (name.equalsIgnoreCase(PROP_REPOSITORY_URL)) {
                        log.log(Level.FINE, "repositoryUrl: {0}", value);
                        repositoryUrl = value;
                    } else if (name.equalsIgnoreCase(PROP_REPOSITORY_BUCKET)) {
                        log.log(Level.FINE, "repositoryBucket: {0}", value);
                        repositoryBucket = value;
                    } else if (name.equalsIgnoreCase(PROP_REPOSITORY_USERNAME)) {
                        log.log(Level.FINE, "repositoryUsername: {0}", value);
                        repositoryUsername = value;
                    } else if (name.equalsIgnoreCase(PROP_REPOSITORY_PASSWORD)) {
                        log.log(Level.FINE, "repositoryPassword: {0}", value);
                        repositoryPassword = value;
                    }
                }
            }
        }
        log.fine("MemManagerStrategyBuilder.readWebAppParams: exit");
    }
}
