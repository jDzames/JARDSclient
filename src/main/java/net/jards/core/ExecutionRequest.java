package net.jards.core;

import java.util.UUID;

/**
 * General class used in method calls and operation requests.
 * Holds needed data for system to execute selected operation. Created in Storage.
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

	private String methodName;

    private String subscriptionName;

    private int remoteCallsId;

	private Transaction transaction;

	private ExecutionContext context;

	private Object[] attributes;

    private String seed;

	private TransactionRunnable runnable;

    private RequestType requestType;

    private boolean waiting = false;

	public ExecutionRequest(Transaction transaction) {
		this.transaction = transaction;
		id = UUID.randomUUID().toString();
		methodName = "";
        subscriptionName = "";
	}

    public boolean isExecuteLocally() {
        return this.requestType == RequestType.ExecuteLocally;
    }

    public boolean isExecute() {
        return this.requestType == RequestType.Execute;
    }
    public boolean isCall() {
        return this.requestType == RequestType.Call;
    }

    public boolean isSubscribe(){
        return this.requestType == RequestType.Subscribe;
    }

    public boolean isUnsubscribe(){
        return this.requestType == RequestType.Unsubscribe;
    }

    public boolean isCompleted() {
		return false;
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

    void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public void setRemoteCallsId(int remoteCallsId) {
        this.remoteCallsId = remoteCallsId;
    }

    void setAttributes(Object[] attributes) {
		this.attributes = attributes;
	}

	void setRunnable(TransactionRunnable runnable) {
		this.runnable = runnable;
	}

	void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	void setContext(ExecutionContext context) {
		this.context = context;
	}

	void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    void setSeed(String seed) {
        this.seed = seed;
    }

    String getSeed() {
        return seed;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public int getRemoteCallsId() {
        return remoteCallsId;
    }

    public Object[] getAttributes() {
		return attributes;
	}

	String getId() {
		return id;
	}

	public String getMethodName() {
		return methodName;
	}

	ExecutionContext getContext() {
		return context;
	}

	Transaction getTransaction() {
		return transaction;
	}

	TransactionRunnable getRunnable() {
		return runnable;
	}

	public RequestType getRequestType() {
        return requestType;
    }
}


/*
* tazko si predtavit co tu ma byt
*
* */