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
package es.rickyepoderi.managertest.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

/**
 * <p>Tester for the web services of the manager-test application. It lets us
 * test the couchbase manager using web services.</p>
 * 
 * @author ricky
 */
public class Tester {
    
    public static final String DEFAULT_BASE_URL = "http://localhost:8080/manager-test/SessionTest?wsdl";
    public static final String DEFAULT_NAMESPACE = "http://server.managertest.rickyepoderi.es/";
    public static final String DEFAULT_LOCAL_PART = "SessionTest";
    
    public static final int DEFAULT_NUMBER_THREADS = 1;
    public static final int DEFAULT_NUMBER_CHILD_THREADS = 1;
    
    public static final int DEFAULT_NUMBER_ATTRIBUTES = 10;
    public static final int DEFAULT_SIZE_ATTRIBUTE = 100;
    
    public static final int DEFAULT_UPDATE_RATIO = 50;
    public static final int DEFAULT_OPERATION_SLEEP_TIME = 0;
    public static final int DEFAULT_THREAD_SLEEP_TIME = 0;
    
    public static final int DEFAULT_ITERATIONS = 1;
    public static final int DEFAULT_CHILD_ITERATIONS = 1;
    
    private String baseUrl = DEFAULT_BASE_URL;
    private String namespace = DEFAULT_NAMESPACE;
    private String localPart = DEFAULT_LOCAL_PART;
    
    private int numThreads = DEFAULT_NUMBER_THREADS;
    private int numChildThreads = DEFAULT_NUMBER_CHILD_THREADS;
    
    private int numAttrs = DEFAULT_NUMBER_ATTRIBUTES;
    private int sizeAttr = DEFAULT_SIZE_ATTRIBUTE;
    
    private int updateRatio = DEFAULT_UPDATE_RATIO;
    private int operationSleep = DEFAULT_OPERATION_SLEEP_TIME;
    private int threadSleep = DEFAULT_THREAD_SLEEP_TIME;
    
    private int iterations = DEFAULT_ITERATIONS;
    private int childIterations = DEFAULT_CHILD_ITERATIONS;
    
    private boolean debug = false;
    
    public enum OperationType {CREATE, UPDATE, REFRESH, DELETE};
    
    private long statCount[] = new long[OperationType.values().length];
    private double statSum[] = new double[OperationType.values().length];
    private double statSumSquares[] = new double[OperationType.values().length];
    private long errors[] = new long[OperationType.values().length];
    private OperationResult[] results = new OperationResult[OperationType.values().length];
    
    private static SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss.SSS");
    
    public class OperationResult {
        private long error;
        private long count;
        private double mean;
        private double dev;

        protected OperationResult() {
            this.error = 0;
            this.count = 0;
            this.mean = 0.0;
            this.dev = 0.0;
        }
        
        protected OperationResult(long error, long count, double mean, double dev) {
            this.error = error;
            this.count = count;
            this.mean = mean;
            this.dev = dev;
        }
        
        public long getError() {
            return error;
        }
        
        public long getCount() {
            return count;
        }

        public double getMean() {
            return mean;
        }

        public double getDev() {
            return dev;
        }
    }
    
    public class ExecutorChild extends Thread {
        private SessionTest proxy = null;
        private int iters = 0;
        private Random random = null;
        private ExecutorParent parent = null;
        
        public ExecutorChild(SessionTest proxy, ExecutorParent parent) {
            this.proxy = proxy;
            this.random = new Random();
            this.iters = 0;
            this.parent = parent;
        }
        
        @Override
        public void run() {
            while (iters < childIterations) {
                if (random.nextInt(100) < updateRatio) {
                    performOperation(parent, this, OperationType.UPDATE, proxy, random);
                } else {
                    performOperation(parent, this, OperationType.REFRESH, proxy, random);
                }
                iters++;
            }
        }
    }
    
    
    public class ExecutorParent extends Thread {
        
        private SessionTest proxy = null;
        private SessionTest_Service service = null;
        private ExecutorChild[] children = null;
        private int iters = 0;
        
        public ExecutorParent() throws MalformedURLException {
            this.service = new SessionTest_Service(
                    new URL(baseUrl),
                    new QName(namespace, localPart));
            this.proxy = service.getSessionTestPort();
            ((BindingProvider)this.proxy).getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
            this.iters = 0;
        }

        @Override
        public void run() {
            while (iters < iterations) {
                // create the session
                performOperation(this, null, OperationType.CREATE, proxy, null);
                // create childs
                children = new ExecutorChild[numChildThreads];
                for (int i = 0; i < children.length; i++) {
                    children[i] = new ExecutorChild(proxy, this);
                    children[i].start();
                }
                for (int i = 0; i < children.length; i++) {
                    try {children[i].join();} catch(Exception e) {}
                }
                // delete the session
                performOperation(this, null, OperationType.DELETE, proxy, null);
                iters++;
            }
        }
    }
    
    //
    // CONSTRUCTOR
    //
    
    /**
     * Constructor default Tester with defaults.
     */
    public Tester() {
        // defaults
    }
    
    /**
     * Constructor of the test via arguments.
     * @param args The arguments
     */
    public Tester(String[] args) {
        parseArguments(args);
    }
    
    //
    // GETTERS & SETTERS
    //
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getLocalPart() {
        return localPart;
    }

    public void setLocalPart(String localPart) {
        this.localPart = localPart;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getNumChildThreads() {
        return numChildThreads;
    }

    public void setNumChildThreads(int numChildThreads) {
        this.numChildThreads = numChildThreads;
    }

    public int getNumAttrs() {
        return numAttrs;
    }

    public void setNumAttrs(int numAttrs) {
        this.numAttrs = numAttrs;
    }

    public int getSizeAttr() {
        return sizeAttr;
    }

    public void setSizeAttr(int sizeAttr) {
        this.sizeAttr = sizeAttr;
    }

    public int getUpdateRatio() {
        return updateRatio;
    }

    public void setUpdateRatio(int updateRatio) {
        this.updateRatio = updateRatio;
    }

    public int getOperationSleep() {
        return operationSleep;
    }

    public void setOperationSleep(int operationSleep) {
        this.operationSleep = operationSleep;
    }

    public int getThreadSleep() {
        return threadSleep;
    }

    public void setThreadSleep(int threadSleep) {
        this.threadSleep = threadSleep;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public int getChildIterations() {
        return childIterations;
    }

    public void setChildIterations(int childIterations) {
        this.childIterations = childIterations;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public OperationResult getResult(OperationType type) {
        return this.results[type.ordinal()];
    }
    
    public long getTotalErrors() {
        long totalErrors = 0;
        for (OperationType type: OperationType.values()) {
            totalErrors += this.results[type.ordinal()].getError();
        }
        return totalErrors;
    }
    
    //
    // STATS methods
    //
    
    /**
     * Reset the test counters for a new execution.
     */
    synchronized private void resetCounters() {
        for (OperationType type: OperationType.values()) {
            int idx = type.ordinal();
            results[idx] = new OperationResult();
            errors[idx] = 0;
            statCount[idx] = 0;
            statSum[idx] = 0.0;
            statSumSquares[idx] = 0.0;
        }
    }
    
    /**
     * Calculate the stats
     * @param result The result from the WS
     * @param type The type of the operation
     * @param sample The sample time of the operation
     */
    synchronized private void calculateStats(String result, OperationType type, long sample) {
        //System.err.println(type + ": " + sample);
        int idx = type.ordinal();
        if (result.contains("ERROR")) {
            errors[idx]++;
        }
        statCount[idx]++;
        double doubleSample = (double) sample;
        statSum[idx] += doubleSample;
        statSumSquares[idx] += doubleSample * doubleSample;
    }
    
    /**
     * Calculate the results after the tests
     */
    private void calculateResults() {
        for (OperationType type: OperationType.values()) {
            int idx = type.ordinal();
            double mean = statSum[idx] / statCount[idx];
            double deviation = Math.sqrt((statSumSquares[idx] / statCount[idx]) - (mean * mean));
            this.results[idx] = new OperationResult(errors[idx], statCount[idx], mean, deviation);
        }
    }
    
    /**
     * Return the argument list.
     * @return 
     */
    public String getArgumemts() {
        return new StringBuilder()
                .append("-b ").append(baseUrl)
                .append(" -n ").append(namespace)
                .append(" -l ").append(localPart)
                .append(" -t ").append(numThreads)
                .append(" -ct ").append(numChildThreads)
                .append(" -a ").append(numAttrs)
                .append(" -s ").append(sizeAttr)
                .append(" -ur ").append(updateRatio)
                .append(" -os ").append(operationSleep)
                .append(" -ts ").append(threadSleep)
                .append(" -i ").append(iterations)
                .append(" -ci ").append(childIterations)
                .append(" -d ").append(debug)
                .toString();
    }
    
    /**
     * Print the result to the standard output.
     */
    public void printResults() {
        long totalErrors = 0;
        System.out.println("Execution: " + getArgumemts());
        for (OperationType type: OperationType.values()) {
            System.out.println(type);
            int idx = type.ordinal();
            System.out.println(" error: " + this.results[idx].getError());
            System.out.println(" count: " + this.results[idx].getCount());
            System.out.println("  mean: " + this.results[idx].getMean());
            System.out.println("   dev: " + this.results[idx].getDev());
            totalErrors += this.results[idx].getError();
        }
        System.out.println("TOTAL ERRORS: " + totalErrors);
    }
    
    /**
     * Method that executes the operation type taking note of the time.
     * @param p The executor parent
     * @param c The executor child
     * @param type The type of operation to perform
     * @param proxy The proxy of the web services
     * @param random A random with the attr number to update or read
     */
    private void performOperation(ExecutorParent p, ExecutorChild c, 
            OperationType type, SessionTest proxy, Random random) {
        StringBuilder sb = null;
        if (debug) {
            sb = new StringBuilder()
                    .append(format.format(new Date()))
                    .append(" (")
                    .append(p.getClass().getSimpleName())
                    .append("-")
                    .append(p.getId())
                    .append("|")
                    .append((c == null)? "null":c.getClass().getSimpleName() + "-" + c.getId())
                    .append("): ")
                    .append(type);
        }
        long start = System.currentTimeMillis();
        String res = null;
        try {
            if (OperationType.UPDATE.equals(type)) {
                res = proxy.updateSession(random.nextInt(numAttrs), sizeAttr, operationSleep);
            } else if (OperationType.REFRESH.equals(type)) {
                res = proxy.refreshSession(operationSleep);
            } else if (OperationType.CREATE.equals(type)) {
                res = proxy.createSession(numAttrs, sizeAttr);
            } else if (OperationType.DELETE.equals(type)) {
                res = proxy.deleteSession();
            }
        } catch (Exception e) {
            res = "ERROR client: Exception doing operation!!!";
        }
        long time = System.currentTimeMillis() - start;
        calculateStats(res, type, time);
        if (debug) {
            sb.append(" res=").append(res).append(" time=").append(time);
            System.err.println(sb);
        }
        if (threadSleep > 0) {
            try {
                Thread.sleep(threadSleep);
            } catch (Exception e) {
            }
        }
    }
    
    /**
     * Throws a IllegalArgumentException explaining the usage.
     * @param error The error to add to the usage
     * @throws IllegalArgumentException It is always thrown
     */
    private static void usage(String error) {
        StringBuilder sb = new StringBuilder();
        if (error != null) {
            sb.append(error);
            sb.append(System.getProperty("line.separator"));
            sb.append(System.getProperty("line.separator"));
        }
        
        sb.append("java ");
        sb.append(Tester.class.getName());
        sb.append(" [-option [value]] ...");
        sb.append(System.getProperty("line.separator"));
        sb.append("Options:");
        sb.append(System.getProperty("line.separator"));
        
        sb.append("  -b: Base URL for the WSDL (default: ");
        sb.append(DEFAULT_BASE_URL);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        sb.append("  -n: Namespace of the WSDL (default: ");
        sb.append(DEFAULT_NAMESPACE);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        sb.append("  -l: Local part of the WSDL (default: ");
        sb.append(DEFAULT_LOCAL_PART);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        
        sb.append("  -t: Number of threads (default: ");
        sb.append(DEFAULT_NUMBER_THREADS);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        sb.append("  -ct: Number of children threads per parent thread (default: ");
        sb.append(DEFAULT_NUMBER_CHILD_THREADS);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        
        sb.append("  -a: Number of attributes in session (default: ");
        sb.append(DEFAULT_NUMBER_ATTRIBUTES);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        sb.append("  -s: Size of each attribute in bytes (default: ");
        sb.append(DEFAULT_SIZE_ATTRIBUTE);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        
        sb.append("  -ur: Ratio (percentage) of updates 0-100 (default: ");
        sb.append(DEFAULT_UPDATE_RATIO);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        sb.append("  -os: Sleep time inside operation in ms (default: ");
        sb.append(DEFAULT_OPERATION_SLEEP_TIME);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        sb.append("  -ts: Sleep time inside thread between operation in ms (default: ");
        sb.append(DEFAULT_THREAD_SLEEP_TIME);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        
        sb.append("  -i: Number of iterations (default: ");
        sb.append(DEFAULT_ITERATIONS);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        sb.append("  -ci: Number of iterations/operations for each parent iteration (default: ");
        sb.append(DEFAULT_CHILD_ITERATIONS);
        sb.append(")");
        sb.append(System.getProperty("line.separator"));
        
        sb.append("  -d: Show debug for threads (default: false)");
        sb.append(System.getProperty("line.separator"));
        
        throw new IllegalArgumentException(sb.toString());
    }
    
    /**
     * Checker for a string argument value (it is present).
     * @param option The option used
     * @param args The list of arguments
     * @param idx The index of this argument
     * @return The argument value if not an error
     */
    private static String checkStringArgument(String option, String[] args, int idx) {
        if (idx >= args.length) {
            usage("Value for option " + option + " not passed");
        }
        return args[idx];
    }
    
    /**
     * Checker for an integer argument. It checks it is passed and the
     * string is an integer between the specified limits.
     * @param option The option used
     * @param args The list of arguments
     * @param idx The index of the value
     * @param min The minimum integer accepted
     * @param max The maximum integer accepted
     * @return The integer value
     */
    private static int checkIntegerArgument(String option, String[] args, int idx, int min, int max) {
        if (idx >= args.length) {
            usage("Value for option " + option + " not passed");
        }
        int res = 0;
        try {
            res = Integer.parseInt(args[idx]);
        } catch(NumberFormatException e) {
            usage("Value for option " + option + " is not a number");
        }
        if (res < min) {
             usage("Value for option " + option + " should be greater or equal to " + min);
        }
        if (res > max) {
             usage("Value for option " + option + " should be less or equal to " + max);
        }
        return res;
    }
    
    /**
     * Parse the arguments creating the test object.
     * @param args The arguments.
     */
    private void parseArguments(String[] args) {
        int i = 0;
        while (i < args.length) {
            if ("-b".equals(args[i])) {
                baseUrl = checkStringArgument("-b", args, i+1);
            } else if ("-n".equals(args[i])) {
                namespace = checkStringArgument("-n", args, i+1);
            } else if ("-l".equals(args[i])) {
                localPart = checkStringArgument("-l", args, i+1);
            } else if ("-t".equals(args[i])) {
                numThreads = checkIntegerArgument("-t", args, i+1, 1, Integer.MAX_VALUE);
            } else if ("-ct".equals(args[i])) {
                numChildThreads = checkIntegerArgument("-ct", args, i+1, 1, Integer.MAX_VALUE);
            } else if ("-a".equals(args[i])) {
                numAttrs = checkIntegerArgument("-a", args, i+1, 1, Integer.MAX_VALUE);
            } else if ("-s".equals(args[i])) {
                sizeAttr = checkIntegerArgument("-s", args, i+1, 1, Integer.MAX_VALUE);
            } else if ("-ur".equals(args[i])) {
                updateRatio = checkIntegerArgument("-ur", args, i+1, 0, 100);
            } else if ("-os".equals(args[i])) {
                operationSleep = checkIntegerArgument("-os", args, i+1, 0, Integer.MAX_VALUE);
            } else if ("-ts".equals(args[i])) {
                threadSleep = checkIntegerArgument("-ts", args, i+1, 0, Integer.MAX_VALUE);
            } else if ("-i".equals(args[i])) {
                iterations = checkIntegerArgument("-i", args, i+1, 1, Integer.MAX_VALUE);
            } else if ("-ci".equals(args[i])) {
                childIterations = checkIntegerArgument("-ci", args, i+1, 1, Integer.MAX_VALUE);
            } else if ("-d".equals(args[i])) {
                debug = true;
                i--;
            } else {
                usage("Unknown option: " + args[i]);
            }
            i = i + 2;
        }
    }
    
    /**
     * Executes the test.
     * @throws Exception Some error in the test
     */
    public void test() throws Exception {
        ExecutorParent[] threads = new ExecutorParent[numThreads];
        this.resetCounters();
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ExecutorParent();
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (Exception e) {
            }
        }
        this.calculateResults();
    }
    
    /**
     * Executes the test.
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        Tester test = new Tester(args);
        test.test();
        test.printResults();
    }
}
