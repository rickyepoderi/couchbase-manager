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
package es.rickyepoderi.couchbasemanager.couchbase;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;

/**
 *
 * Class that represents any couchbase result operation. This is composed
 * by two fixed element OperationStatus and the type (the type is the same
 * than the ClientRequest which originated the result) and two optional
 * elements: cas and object (only some operations returns a cas or an object).
 * 
 * @author ricky
 */
public class ClientResult {

    /**
     * OperationStatus set when an exception is thrown
     */
    static public final OperationStatus EXCEPTION = new OperationStatus(false, "EXCEPTION");
    
    /**
     * The type of the operation. It is the same type than the request which
     * creates the result.
     */
    private OperationType type = null;
    
    /**
     * The status of the operation. It always exists in the result (cannot be null).
     */
    private OperationStatus status = null;
    
    /**
     * CAS of the operation. It is only produced in get operations.
     */
    private long cas = -1;
    
    /**
     * The object for get operations that return an object.
     */
    private Object value = null;
    
    /**
     * The key used in the operation.
     */
    private String key = null;
    
    /**
     * The exception that generated the EXCEPTION status
     */
    private Throwable exception = null;

    /**
     * Empty constructor. It is private cos it can only be called using
     * the static methods.
     */
    private ClientResult() {
        this.type = null;
        this.status = null;
        this.cas = -1;
        this.value = null;
        this.key = null;
    }
    
    /**
     * Constructor via type. Also private to not be called outside the class.
     * @param type The type of the operation performed
     */
    private ClientResult(OperationType type) {
        this();
        this.type = type;
    }
    
    /**
     * Constructor via all the properties. Also private to avoid direct calling
     * outside the class.
     * @param type The type of the operation.
     * @param status The status produced by the operation.
     * @param cas The cas returned by couchbase
     * @param value The object returned by couchbase
     * @param key The key value
     */
    private ClientResult(OperationType type, OperationStatus status, long cas, Object value, String key) {
        this(type);
        this.status = status;
        this.cas = cas;
        this.value = value;
        this.key = key;
    }
    
    /**
     * Create a result for the operations that return a OperationFuture&lt;CASValue&lt;Object&gt;&gt;.
     * Method are protected cos they can only be used inside the package.
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @param type The type of the operation
     * @param future The future returned by the couchbase
     * @return The result for the operation
     */
    protected static ClientResult createClientResultObject(long timeout, OperationType type, OperationFuture<CASValue<Object>> future) {
        ClientResult res = new ClientResult(type);
        try {
            CASValue<Object> casValue = future.get(timeout, TimeUnit.MILLISECONDS);
            res.status = future.getStatus();
            res.key = future.getKey();
            if (res.status.isSuccess()) {
                res.cas = future.get().getCas();
                res.value = casValue.getValue();
            } else {
                res.cas = -1;
                res.value = null;
            }
        } catch (Exception e) {
            res.status = EXCEPTION;
            res.cas = -1;
            res.value = null;
            res.key = null;
            res.exception = e;
        }
        return res;
    }

    /**
     * Create a result for the operations that return a OperationFuture&lt;CASResponse&gt;.
     * Method are protected cos they can only be used inside the package.
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @param type The type of the operation
     * @param future The future returned by couchbase
     * @return The result for this operation
     */
    protected static ClientResult createClientResultCas(long timeout, OperationType type, OperationFuture<CASResponse> future) {
        ClientResult res = new ClientResult(type);
        res.cas = -1;
        res.value = null;
        try {
            CASResponse cas = future.get(timeout, TimeUnit.MILLISECONDS);
            res.key = future.getKey();
            res.cas = future.getCas();
            if (CASResponse.OK.equals(cas)) {
                res.status = new OperationStatus(true, "CAS OK!");
            } else if (CASResponse.NOT_FOUND.equals(cas)) {
                res.status = new OperationStatus(false, "NOT_FOUND");
            } else if (CASResponse.EXISTS.equals(cas)) {
                res.status = new OperationStatus(false, "EXISTS");
            } else {
                res.status = new OperationStatus(false, "CAS UNKNOWN ERROR: " + cas);
            }
        } catch (Throwable e) {
            res.status = EXCEPTION;
            res.exception = e;
        }
        return res;
    }
    
    /**
     * Create a result for the operations that return a OperationFuture&lt;Boolean&gt;.
     * Method are protected cos they can only be used inside the package.
     * @param timeout Timeout to wait for the operation to finish (in ms)
     * @param type The type of the operation
     * @param future The future returned by couchbase
     * @return The result for this operation
     */
    protected static ClientResult createClientResultOperation(long timeout, OperationType type, OperationFuture<Boolean> future) {
        ClientResult res = new ClientResult(type);
        if (!OperationType.UNLOCK.equals(type) && !OperationType.TOUCH.equals(type)) {
            res.cas = future.getCas();
        }
        res.value = null;
        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
            res.status = future.getStatus();
            res.key = future.getKey();
        } catch (Exception e) {
            res.status = EXCEPTION;
            res.exception = e;
        }
        return res;
    }

    /**
     * Getter for CAS.
     * @return The cas of the operation or -1
     */
    public long getCas() {
        return cas;
    }

    /**
     * Getter for the operation status.
     * @return The operation status (never null)
     */
    public OperationStatus getStatus() {
        return status;
    }

    /**
     * Getter for the type.
     * @return The type of the operation (never null)
     */
    public OperationType getType() {
        return type;
    }

    /**
     * Getter for the object returned by couchbase in get operations.
     * @return The object or null
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * The key used in the operation.
     * @return The key used
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Getter for the exception if produced.
     * @return The exception
     */
    public Throwable getException() {
        return exception;
    }
    
    /**
     * Checks if the operations is a success.
     * @return true if success, false otherwise.
     */
    public boolean isSuccess() {
        return status.isSuccess();
    }
    
    /**
     * Checks if the operation returns a NOT_FOUND (object does not exist in
     * couchbase).
     * @return true if the error is not found, false otherwise
     */
    public boolean isNotFound() {
        return status.getMessage().equals("NOT_FOUND") || 
                status.getMessage().equals("Not found");
    }
    
    /**
     * Checks if the status returned by the operation is a LOCK_ERROR (the
     * object is currently locked by another server).
     * @return true if the operation returned a lock error, false otherwise
     */
    public boolean isLockError() {
        return status.getMessage().equals("LOCK_ERROR");
    }
    
    /**
     * String representation of the response.
     * @return The string representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append(": ");
        sb.append(this.type);
        sb.append(" ");
        sb.append(this.status.getMessage());
        sb.append(" CAS=");
        sb.append(this.cas);
        sb.append(" Value=");
        sb.append(this.value);
        return sb.toString();
    }
    
}
