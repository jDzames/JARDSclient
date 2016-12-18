package net.jards.core;

public interface TransactionRunnable {

	void run(ExecutionContext context, Transaction transaction, Object... arguments);

}
