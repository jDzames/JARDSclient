package net.jards.core;

/**
 * Interface for context used in TransactionRunnable providing needed components.
 */
public interface ExecutionContext {

    /**
     * Method that returns collection that can be used to another work.
     * @param name colelction name
     * @return specified collection
     */
    public Collection getCollection(String name);
	
}
