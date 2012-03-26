/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
                ((CouchbaseManager) manager).doSessionSave((CouchbaseWrapperSession) session);
            }
        }
        log.fine("MemManagerValve.postInvoke: exit");
    }
    
}
