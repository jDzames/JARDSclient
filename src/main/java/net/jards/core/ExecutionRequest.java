package net.jards.core;

import java.util.UUID;

/**
 * General class used in method calls and operation requests.
 * Holds needed data for system to execute selected operation. Created in Storage.
 * Later referred to this as to "request".
 */
public class ExecutionRequest {

    /**
     * Type of request : ExecuteLocally, Execute, Call, Subscribe, Unsubscribe.
     */
    public enum RequestType{
        ExecuteLocally,
        Execute,
        Call,
        Subscribe,
        Unsubscribe
    }

	/**
	 * Unique identifier of the request.
	 */
	private final String id;

    /**
     * Name of method that will be called on server (speculation with same name used locally).
     */
    private String methodName;

    /**
     * Name of subscription if it's type subscribe
     */
    private String subscriptionName;

    /**
     * id that can be given to this request by remote storage (used in DDP) to find this after confirmation
     */
    private int remoteCallsId;

    /**
     * transaction for this request (with possible DocumentChanges after execution)
     */
    private Transaction transaction;

    /**
     * context for this request
     */
    private ExecutionContext context;

    /**
     * User specified attributes
     */
    private Object[] attributes;

    /**
     * Seed given to this request by Storage
     */
    private String seed;

    /**
     * User specified runnable that will be executed
     */
    private TransactionRunnable runnable;

    /**
     * Type of this request
     */
    private RequestType requestType;

    /**
     * boolean variable for await method
     */
    private boolean waiting = false;

    /**
     * Public constructor with transaction (ExecutionRequest should always be created by system)
     * @param transaction transaction for this request
     */
    public ExecutionRequest(Transaction transaction) {
		this.transaction = transaction;
		id = UUID.randomUUID().toString();
		methodName = "";
        subscriptionName = "";
	}

    /**
     * @return true if this request is of type ExecuteLocally
     */
    public boolean isExecuteLocally() {
        return this.requestType == RequestType.ExecuteLocally;
    }

    /**
     * @return true if this request is of type Execute
     */
    public boolean isExecute() {
        return this.requestType == RequestType.Execute;
    }

    /**
     * @return true if this request is of type Call
     */
    public boolean isCall() {
        return this.requestType == RequestType.Call;
    }

    /**
     * @return true if this request is of type Subscribe
     */
    public boolean isSubscribe(){
        return this.requestType == RequestType.Subscribe;
    }

    /**
     * @return true if this request is of type Unsubscribe
     */
    public boolean isUnsubscribe(){
        return this.requestType == RequestType.Unsubscribe;
    }

	/**
	 * Wait for completing execution request.
	 */
    void await() {
        waiting = true;
        synchronized (this) {
            try {
                while (waiting) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
	}

    /**
     * Set ready when execution is done, wake up from waiting.
     */
    void ready(){
        waiting = false;
        synchronized (this){
            this.notify();
        }
    }

    /**
     * @param subscriptionName name specified to this subscription by user
     */
    void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    /**
     * @param remoteCallsId id that can be specified by RemoteStorage implementation
     */
    public void setRemoteCallsId(int remoteCallsId) {
        this.remoteCallsId = remoteCallsId;
    }

    /**
     * @param attributes given attributes by user
     */
    void setAttributes(Object[] attributes) {
		this.attributes = attributes;
	}

    /**
     * @param runnable runnable specified by user (set here by Storage)
     */
    void setRunnable(TransactionRunnable runnable) {
		this.runnable = runnable;
	}

    /**
     * @param methodName name for remote call (simulation name)
     */
    void setMethodName(String methodName) {
		this.methodName = methodName;
	}

    /**
     * @param context context set for this request by Storage
     */
    void setContext(ExecutionContext context) {
		this.context = context;
	}

    /**
     * @param requestType type of index (specified in appropriate method in Storage)
     */
    void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    /**
     * @param seed for this request (used in speculation)
     */
    void setSeed(String seed) {
        this.seed = seed;
    }

    /**
     * @return seed of this request
     */
    String getSeed() {
        return seed;
    }

    /**
     * @return name of this subscription (empty string if its not subscribe type)
     */
    public String getSubscriptionName() {
        return subscriptionName;
    }

    /**
     * @return id of remote call if was specified
     */
    public int getRemoteCallsId() {
        return remoteCallsId;
    }

    /**
     * @return attributes for this request
     */
    public Object[] getAttributes() {
		return attributes;
	}

    /**
     * @return general id of this request
     */
    String getId() {
		return id;
	}

    /**
     * @return method name of this request (empty string if it is not call type)
     */
    public String getMethodName() {
		return methodName;
	}

    /**
     * @return context of this request
     */
    ExecutionContext getContext() {
		return context;
	}

    /**
     * @return transaction of this request
     */
    Transaction getTransaction() {
		return transaction;
	}

    /**
     * @return runnable that will be used
     */
    TransactionRunnable getRunnable() {
		return runnable;
	}

    /**
     * @return this request type
     */
    public RequestType getRequestType() {
        return requestType;
    }
}
