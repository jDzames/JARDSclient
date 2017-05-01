package net.jards.core;


/**
 * Interface for methods that can make changes to local storage.
 *
 * Implement this class to add, update, remove or find documents.
 * Then use it with storage methods (execute or call speculation).
 */
public interface TransactionRunnable {

    /**
     * Method where users can make their operations with documents (add, update, remove, find) through collection.
     * @param context context where user can get collection
     * @param transaction transaction that executes given changes
     * @param arguments optional arguments
     */
    void run(ExecutionContext context, Transaction transaction, Object... arguments);

}
