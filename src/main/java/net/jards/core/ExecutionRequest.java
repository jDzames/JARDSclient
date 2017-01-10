package net.jards.core;

public class ExecutionRequest {

	/**
	 * Unique identifier of the request.
	 */
	private final String id;

	private String methodName;

	private Transaction transaction;

	private Object[] attributes;

	private TransactionRunnable runnable;

	private boolean local;

	public ExecutionRequest(Transaction transaction) {
		this.transaction = transaction;
		id = "123";
	}

	public boolean isCompleted() {
		return false;
	}

	/**
	 * Wait for completing execution request.
	 */
	public void await() {

	}

}


/*
* tazko si predtavit co tu ma byt
*
* */