package net.jards.core;

public class ExecutionRequest {

	/**
	 * Unique identifier of the request.
	 */
	private final String id;

	private String methodName;

	private Transaction transaction;

	private ExecutionContext context;

	private Object[] attributes;

	private TransactionRunnable runnable;

	private boolean local;
    private boolean speculation;

	public ExecutionRequest(Transaction transaction) {
		this.transaction = transaction;
		id = "" + (int) Math.random()*1000000;
		methodName = "";
		local = false;
	}

	public boolean isCompleted() {
		return false;
	}

	/**
	 * Wait for completing execution request.
	 */
	public void await() {

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

	void setLocal(boolean local) {
		this.local = local;
	}

    void setSpeculation(boolean speculation) {
        this.speculation = speculation;
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

	boolean isLocal() {
		return local;
	}

    boolean isSpeculation() {
        return speculation;
    }
}


/*
* tazko si predtavit co tu ma byt
*
* */