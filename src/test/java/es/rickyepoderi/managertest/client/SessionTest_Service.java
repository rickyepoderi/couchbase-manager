
package es.rickyepoderi.managertest.client;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "SessionTest", targetNamespace = "http://server.managertest.rickyepoderi.es/", wsdlLocation = "http://localhost:8080/manager-test/SessionTest?wsdl")
public class SessionTest_Service
    extends Service
{

    private final static URL SESSIONTEST_WSDL_LOCATION;
    private final static WebServiceException SESSIONTEST_EXCEPTION;
    private final static QName SESSIONTEST_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "SessionTest");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8080/manager-test/SessionTest?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        SESSIONTEST_WSDL_LOCATION = url;
        SESSIONTEST_EXCEPTION = e;
    }

    public SessionTest_Service() {
        super(__getWsdlLocation(), SESSIONTEST_QNAME);
    }

    public SessionTest_Service(WebServiceFeature... features) {
        super(__getWsdlLocation(), SESSIONTEST_QNAME, features);
    }

    public SessionTest_Service(URL wsdlLocation) {
        super(wsdlLocation, SESSIONTEST_QNAME);
    }

    public SessionTest_Service(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, SESSIONTEST_QNAME, features);
    }

    public SessionTest_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SessionTest_Service(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns SessionTest
     */
    @WebEndpoint(name = "SessionTestPort")
    public SessionTest getSessionTestPort() {
        return super.getPort(new QName("http://server.managertest.rickyepoderi.es/", "SessionTestPort"), SessionTest.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns SessionTest
     */
    @WebEndpoint(name = "SessionTestPort")
    public SessionTest getSessionTestPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://server.managertest.rickyepoderi.es/", "SessionTestPort"), SessionTest.class, features);
    }

    private static URL __getWsdlLocation() {
        if (SESSIONTEST_EXCEPTION!= null) {
            throw SESSIONTEST_EXCEPTION;
        }
        return SESSIONTEST_WSDL_LOCATION;
    }

}
