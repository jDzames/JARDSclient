package net.jards.core;


/**
 * Default ExecutionContext that can provide needed collection by name.
 */
public class DefaultExecutionContext implements ExecutionContext{

    /**
     * Storage reference
     */
    private Storage storage;

    /**
     * Method to set Storage reference
     * @param storage Storage reference (used in your solution)
     */
    DefaultExecutionContext(Storage storage){
        this.storage = storage;
    }

    /**
     * Method to get collection object that can be used further.
     * @param name name of collection
     * @return specified collection
     */
    @Override
    public Collection getCollection(String name) {
        return storage.getCollection(name);
    }
}
