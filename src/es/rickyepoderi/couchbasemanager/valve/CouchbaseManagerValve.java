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
package es.rickyepoderi.couchbasemanager.valve;

import es.rickyepoderi.couchbasemanager.session.CouchbaseManager;
import es.rickyepoderi.couchbasemanager.session.CouchbaseWrapperSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.catalina.Manager;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Session;
import org.apache.catalina.valves.ValveBase;

/**
 *
 * NOT USED!!!
 * 
 * @author ricky
 */
public class CouchbaseManagerValve extends ValveBase {

    /**
     * The logger to use for logging ALL web container related messages.
     */
    protected static final Logger log = Logger.getLogger(CouchbaseManagerValve.class.getName());

    /**
     * Creates a new instance of HASessionStoreValve
     */
    public CouchbaseManagerValve() {
        super();
    }

    @Override
    public int invoke(Request rqst, Response response
            ) throws IOException, ServletException {
        return INVOKE_NEXT;
    }

    @Override
    public void postInvoke(Request request, Response response) throws IOException, ServletException {
        log.fine("MemManagerValve.postInvoke: init");
        Manager manager = request.getContext().getManager();
        if (manager instanceof CouchbaseManager) {
            HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
            HttpSession hsession = hreq.getSession(false);
            if (hsession != null && hsession.getId() != null) {
                log.log(Level.FINE, "MemManagerValve.postInvoke: session id: {0}", hsession.getId());
                Session session = manager.findSession(hsession.getId());
                ((CouchbaseManager) manager).doSessionSave((CouchbaseWrapperSession) session, null);
            }
        }
        log.fine("MemManagerValve.postInvoke: exit");
    }
    
}
