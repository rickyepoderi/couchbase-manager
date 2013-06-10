
package es.rickyepoderi.managertest.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for refreshSession complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="refreshSession">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="sleep" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "refreshSession", propOrder = {
    "sleep"
})
public class RefreshSession {

    protected int sleep;

    /**
     * Gets the value of the sleep property.
     * 
     */
    public int getSleep() {
        return sleep;
    }

    /**
     * Sets the value of the sleep property.
     * 
     */
    public void setSleep(int value) {
        this.sleep = value;
    }

}
