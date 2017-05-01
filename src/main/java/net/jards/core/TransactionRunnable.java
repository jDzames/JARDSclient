package net.jards.core;


public interface TransactionRunnable {

	void run(ExecutionContext context, Transaction transaction, Object... arguments);

}

/*
* metoda, run ako vo vlakne
* pusti sa v RequestHandleTread
*
* */