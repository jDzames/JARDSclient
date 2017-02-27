package net.jards.core;

import java.util.UUID;

public class ExecutionRequest {

    public enum RequestType{
        ExecuteLocally,
        Execute,
        Call,
        Subscribe
    }

	/**
	 * Unique identifier of the request.
	 */
	private final String id;

	private String methodName;

	private Transaction transaction;

    private Subscription subscriptionObject;

	private ExecutionContext context;

	private Object[] attributes;

	private TransactionRunnable runnable;

    private RequestType requestType;

    private boolean waiting = false;

	public ExecutionRequest(Transaction transaction) {
		this.transaction = transaction;
		id = UUID.randomUUID().toString();
		methodName = "";
	}

    boolean isExecuteLocally() {
        return this.requestType == RequestType.ExecuteLocally;
    }

    boolean isExecute() {
        return this.requestType == RequestType.Execute;
    }
    boolean isCall() {
        return this.requestType == RequestType.Call;
    }

    boolean isSubscribe(){
        return this.requestType == RequestType.Subscribe;
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

    void setSubscriptionObject(Subscription subscriptionObject) {
        this.subscriptionObject = subscriptionObject;
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

    Subscription getSubscriptionObject() {
        return subscriptionObject;
    }

    Object[] getAttributes() {
		return attributes;
	}

	String getId() {
		return id;
	}

	String getMethodName() {
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

	RequestType getRequestType() {
        return requestType;
    }
}


/*
* tazko si predtavit co tu ma byt
*
* */