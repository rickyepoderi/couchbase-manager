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

import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.deployment.runtime.web.ManagerProperties;
import com.sun.enterprise.deployment.runtime.web.SessionManager;
import com.sun.enterprise.deployment.runtime.web.WebProperty;
import com.sun.enterprise.web.ServerConfigLookup;
import es.rickyepoderi.couchbasemanager.couchbase.transcoders.GlassfishTranscoderUtil;
import es.rickyepoderi.couchbasemanager.session.CouchbaseManager;
import es.rickyepoderi.couchbasemanager.session.UsageConfiguration;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.spy.memcached.PersistTo;
import net.spy.memcached.ReplicateTo;
import org.apache.catalina.Context;
import org.apache.catalina.core.StandardContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 *
 * <p>Real implementation for glassfish V3. Now the CouchbaseManagerStrategyBuilder
 * is an abstract class with the common properties, and there are a V3 and V4
 * (some classes have moved from one package to another, and injection of
 * JavaEEIOUtils is different). In v3 the manager should be defined in:</p>
 * 
 * <pre>
 * META-INF/inhabitants/default
 * </pre>
 * 
 * <p>In this file it is defined like this:</p>
 * 
 * <pre>
 * class=es.rickyepoderi.couchbasemanager.web.CouchbaseManagerStrategyBuilderV3,index=com.sun.enterprise.web.PersistenceStrategyBuilder:coherence-web
 * </pre>
 * 
 * @author ricky
 */
@Service(name = "coherence-web")
@Scoped(PerLookup.class)
public class CouchbaseManagerStrategyBuilderV3 extends CouchbaseManagerStrategyBuilder {

    /**
     * logger for the class
     */
    protected static final Logger log = Logger.getLogger(CouchbaseManagerStrategyBuilderV3.class.getName());
        
    /**
     * The ioUtils from glassfish package that are going to be used for getting
     * the Object input/output streams. It is used in the AppSerializingTranscoder.
     */
    @Inject
    private JavaEEIOUtils ioUtils;

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
        manager.setSticky(sticky);
        manager.setLockTime(lockTime);
        manager.setOperationTimeout(operationTimeout);
        manager.setPersistTo(persistTo);
        manager.setReplicateTo(replicateTo);
        manager.setAttrMaxSize(attrMaxSize);
        manager.setAttrTouchExtraTime(attrTouchExtraTime);
        manager.setAttrUsageCondition(attrUsageCondition);
        log.log(Level.FINE, "MemManagerStrategyBuilder.initializePersistenceStrategy: ioUtils={0}", ioUtils);
        GlassfishTranscoderUtil transcoder = new GlassfishTranscoderUtil();
        transcoder.setIoUtils(ioUtils);
        manager.setTranscoder(transcoder);
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
                    } else if (name.equalsIgnoreCase(PROP_STICKY)) {
                        log.log(Level.FINE, "sticky: {0}", value);
                        sticky = Boolean.parseBoolean(value);
                    } else if (name.equalsIgnoreCase(PROP_LOCK_TIME)) {
                        log.log(Level.FINE, "lockTime: {0}", value);
                        try {
                            lockTime = Integer.parseInt(value);
                            if (lockTime <= 0) {
                                log.log(Level.WARNING, "Invalid integer format for lockTime {0}", value);
                                lockTime = DEFAULT_LOCK_TIME;
                            }
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Invalid integer format for lockTime {0}", value);
                        }
                    }  else if (name.equalsIgnoreCase(PROP_OPERATION_TIMEOUT)) {
                        log.log(Level.FINE, "operationTimeout: {0}", value);
                        try {
                            operationTimeout = Long.parseLong(value);
                            if (operationTimeout < 0) {
                                log.log(Level.WARNING, "Invalid long format for operationTimeout {0}", value);
                                operationTimeout = DEFAULT_OPERATION_TIMEOUT;
                            }
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Invalid long format for operationTimeout {0}", value);
                        }
                    } else if (name.equalsIgnoreCase(PROP_PERSIST_TO)) {
                        log.log(Level.FINE, "persistTo: {0}", value);
                        try {
                            persistTo = PersistTo.valueOf(value);
                        } catch (Exception e) {
                            log.log(Level.WARNING, "Invalid PersistTo enum {0}", value);
                        }
                    } else if (name.equalsIgnoreCase(PROP_REPLICATE_TO)) {
                        log.log(Level.FINE, "replicateTo: {0}", value);
                        try {
                            replicateTo = ReplicateTo.valueOf(value);
                        } catch (Exception e) {
                            log.log(Level.WARNING, "Invalid ReplicateTo enum {0}", value);
                        }
                    } else if (name.equalsIgnoreCase(PROP_ATTR_MAX_SIZE)) {
                        log.log(Level.FINE, "attrMaxSize: {0}", value);
                        try {
                            attrMaxSize = Integer.parseInt(value);
                            if (attrMaxSize < 0) {
                                log.log(Level.WARNING, "Invalid int format for attrMaxSize {0}", value);
                                attrMaxSize = DEFAULT_ATTR_MAX_SIZE;
                            }
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Invalid int format for attrMaxSize {0}", value);
                        }
                    } else if (name.equalsIgnoreCase(PROP_ATTR_TOUCH_EXTRA_TIME)) {
                        log.log(Level.FINE, "attrTouchExtraTime: {0}", value);
                        try {
                            attrTouchExtraTime = Integer.parseInt(value);
                            if (attrTouchExtraTime < 0) {
                                log.log(Level.WARNING, "Invalid int format for attrTouchExtraTime {0}", value);
                                attrTouchExtraTime = DEFAULT_ATTR_TOUCH_EXTRA_TIME;
                            }
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Invalid int format for attrTouchExtraTime {0}", value);
                        }
                    } else if (name.equalsIgnoreCase(PROP_ATTR_USAGE_CONDITION)) {
                        log.log(Level.FINE, "attrUsageCondition: {0}", value);
                        try {
                            attrUsageCondition = new UsageConfiguration(value);
                        } catch (NumberFormatException e) {
                            log.log(Level.WARNING, "Invalid int format for attrUsageCondition {0}", value);
                            attrUsageCondition = null;
                        }
                    }
                }
                // assign attr usage if not defined
                if (attrUsageCondition == null && this.sticky) {
                    attrUsageCondition = DEFAULT_USAGE_CONFIGURATION_STICKY;
                } else if (attrUsageCondition == null) {
                    attrUsageCondition = DEFAULT_USAGE_CONFIGURATION_NON_STICKY;
                }
            }
        }
        log.fine("MemManagerStrategyBuilder.readWebAppParams: exit");
    }
}
