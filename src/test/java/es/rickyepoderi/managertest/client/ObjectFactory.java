
package es.rickyepoderi.managertest.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the es.rickyepoderi.managertest.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _AddSessionAttribute_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "addSessionAttribute");
    private final static QName _RefreshSession_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "refreshSession");
    private final static QName _DeleteSessionAttribute_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "deleteSessionAttribute");
    private final static QName _DeleteSessionResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "deleteSessionResponse");
    private final static QName _CheckSessionResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "checkSessionResponse");
    private final static QName _UpdateSeveralSessionResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "updateSeveralSessionResponse");
    private final static QName _CreateSessionResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "createSessionResponse");
    private final static QName _CheckNullSession_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "checkNullSession");
    private final static QName _AddSessionAttributeResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "addSessionAttributeResponse");
    private final static QName _DeleteSession_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "deleteSession");
    private final static QName _UpdateSeveralSession_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "updateSeveralSession");
    private final static QName _CheckNullSessionResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "checkNullSessionResponse");
    private final static QName _DeleteSessionAttributeResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "deleteSessionAttributeResponse");
    private final static QName _CheckSession_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "checkSession");
    private final static QName _UpdateSessionResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "updateSessionResponse");
    private final static QName _UpdateSession_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "updateSession");
    private final static QName _CreateSession_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "createSession");
    private final static QName _RefreshSessionResponse_QNAME = new QName("http://server.managertest.rickyepoderi.es/", "refreshSessionResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: es.rickyepoderi.managertest.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link UpdateSession }
     * 
     */
    public UpdateSession createUpdateSession() {
        return new UpdateSession();
    }

    /**
     * Create an instance of {@link RefreshSessionResponse }
     * 
     */
    public RefreshSessionResponse createRefreshSessionResponse() {
        return new RefreshSessionResponse();
    }

    /**
     * Create an instance of {@link CreateSession }
     * 
     */
    public CreateSession createCreateSession() {
        return new CreateSession();
    }

    /**
     * Create an instance of {@link DeleteSessionAttributeResponse }
     * 
     */
    public DeleteSessionAttributeResponse createDeleteSessionAttributeResponse() {
        return new DeleteSessionAttributeResponse();
    }

    /**
     * Create an instance of {@link CheckSession }
     * 
     */
    public CheckSession createCheckSession() {
        return new CheckSession();
    }

    /**
     * Create an instance of {@link UpdateSessionResponse }
     * 
     */
    public UpdateSessionResponse createUpdateSessionResponse() {
        return new UpdateSessionResponse();
    }

    /**
     * Create an instance of {@link DeleteSession }
     * 
     */
    public DeleteSession createDeleteSession() {
        return new DeleteSession();
    }

    /**
     * Create an instance of {@link UpdateSeveralSession }
     * 
     */
    public UpdateSeveralSession createUpdateSeveralSession() {
        return new UpdateSeveralSession();
    }

    /**
     * Create an instance of {@link CheckNullSessionResponse }
     * 
     */
    public CheckNullSessionResponse createCheckNullSessionResponse() {
        return new CheckNullSessionResponse();
    }

    /**
     * Create an instance of {@link DeleteSessionAttribute }
     * 
     */
    public DeleteSessionAttribute createDeleteSessionAttribute() {
        return new DeleteSessionAttribute();
    }

    /**
     * Create an instance of {@link DeleteSessionResponse }
     * 
     */
    public DeleteSessionResponse createDeleteSessionResponse() {
        return new DeleteSessionResponse();
    }

    /**
     * Create an instance of {@link RefreshSession }
     * 
     */
    public RefreshSession createRefreshSession() {
        return new RefreshSession();
    }

    /**
     * Create an instance of {@link AddSessionAttribute }
     * 
     */
    public AddSessionAttribute createAddSessionAttribute() {
        return new AddSessionAttribute();
    }

    /**
     * Create an instance of {@link UpdateSeveralSessionResponse }
     * 
     */
    public UpdateSeveralSessionResponse createUpdateSeveralSessionResponse() {
        return new UpdateSeveralSessionResponse();
    }

    /**
     * Create an instance of {@link CheckSessionResponse }
     * 
     */
    public CheckSessionResponse createCheckSessionResponse() {
        return new CheckSessionResponse();
    }

    /**
     * Create an instance of {@link AddSessionAttributeResponse }
     * 
     */
    public AddSessionAttributeResponse createAddSessionAttributeResponse() {
        return new AddSessionAttributeResponse();
    }

    /**
     * Create an instance of {@link CheckNullSession }
     * 
     */
    public CheckNullSession createCheckNullSession() {
        return new CheckNullSession();
    }

    /**
     * Create an instance of {@link CreateSessionResponse }
     * 
     */
    public CreateSessionResponse createCreateSessionResponse() {
        return new CreateSessionResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddSessionAttribute }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "addSessionAttribute")
    public JAXBElement<AddSessionAttribute> createAddSessionAttribute(AddSessionAttribute value) {
        return new JAXBElement<AddSessionAttribute>(_AddSessionAttribute_QNAME, AddSessionAttribute.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RefreshSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "refreshSession")
    public JAXBElement<RefreshSession> createRefreshSession(RefreshSession value) {
        return new JAXBElement<RefreshSession>(_RefreshSession_QNAME, RefreshSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteSessionAttribute }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "deleteSessionAttribute")
    public JAXBElement<DeleteSessionAttribute> createDeleteSessionAttribute(DeleteSessionAttribute value) {
        return new JAXBElement<DeleteSessionAttribute>(_DeleteSessionAttribute_QNAME, DeleteSessionAttribute.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "deleteSessionResponse")
    public JAXBElement<DeleteSessionResponse> createDeleteSessionResponse(DeleteSessionResponse value) {
        return new JAXBElement<DeleteSessionResponse>(_DeleteSessionResponse_QNAME, DeleteSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "checkSessionResponse")
    public JAXBElement<CheckSessionResponse> createCheckSessionResponse(CheckSessionResponse value) {
        return new JAXBElement<CheckSessionResponse>(_CheckSessionResponse_QNAME, CheckSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateSeveralSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "updateSeveralSessionResponse")
    public JAXBElement<UpdateSeveralSessionResponse> createUpdateSeveralSessionResponse(UpdateSeveralSessionResponse value) {
        return new JAXBElement<UpdateSeveralSessionResponse>(_UpdateSeveralSessionResponse_QNAME, UpdateSeveralSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "createSessionResponse")
    public JAXBElement<CreateSessionResponse> createCreateSessionResponse(CreateSessionResponse value) {
        return new JAXBElement<CreateSessionResponse>(_CreateSessionResponse_QNAME, CreateSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckNullSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "checkNullSession")
    public JAXBElement<CheckNullSession> createCheckNullSession(CheckNullSession value) {
        return new JAXBElement<CheckNullSession>(_CheckNullSession_QNAME, CheckNullSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddSessionAttributeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "addSessionAttributeResponse")
    public JAXBElement<AddSessionAttributeResponse> createAddSessionAttributeResponse(AddSessionAttributeResponse value) {
        return new JAXBElement<AddSessionAttributeResponse>(_AddSessionAttributeResponse_QNAME, AddSessionAttributeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "deleteSession")
    public JAXBElement<DeleteSession> createDeleteSession(DeleteSession value) {
        return new JAXBElement<DeleteSession>(_DeleteSession_QNAME, DeleteSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateSeveralSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "updateSeveralSession")
    public JAXBElement<UpdateSeveralSession> createUpdateSeveralSession(UpdateSeveralSession value) {
        return new JAXBElement<UpdateSeveralSession>(_UpdateSeveralSession_QNAME, UpdateSeveralSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckNullSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "checkNullSessionResponse")
    public JAXBElement<CheckNullSessionResponse> createCheckNullSessionResponse(CheckNullSessionResponse value) {
        return new JAXBElement<CheckNullSessionResponse>(_CheckNullSessionResponse_QNAME, CheckNullSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DeleteSessionAttributeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "deleteSessionAttributeResponse")
    public JAXBElement<DeleteSessionAttributeResponse> createDeleteSessionAttributeResponse(DeleteSessionAttributeResponse value) {
        return new JAXBElement<DeleteSessionAttributeResponse>(_DeleteSessionAttributeResponse_QNAME, DeleteSessionAttributeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CheckSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "checkSession")
    public JAXBElement<CheckSession> createCheckSession(CheckSession value) {
        return new JAXBElement<CheckSession>(_CheckSession_QNAME, CheckSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "updateSessionResponse")
    public JAXBElement<UpdateSessionResponse> createUpdateSessionResponse(UpdateSessionResponse value) {
        return new JAXBElement<UpdateSessionResponse>(_UpdateSessionResponse_QNAME, UpdateSessionResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UpdateSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "updateSession")
    public JAXBElement<UpdateSession> createUpdateSession(UpdateSession value) {
        return new JAXBElement<UpdateSession>(_UpdateSession_QNAME, UpdateSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CreateSession }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "createSession")
    public JAXBElement<CreateSession> createCreateSession(CreateSession value) {
        return new JAXBElement<CreateSession>(_CreateSession_QNAME, CreateSession.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RefreshSessionResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.managertest.rickyepoderi.es/", name = "refreshSessionResponse")
    public JAXBElement<RefreshSessionResponse> createRefreshSessionResponse(RefreshSessionResponse value) {
        return new JAXBElement<RefreshSessionResponse>(_RefreshSessionResponse_QNAME, RefreshSessionResponse.class, null, value);
    }

}
