package net.jards.core;

/**
 * Created by jDzama on 12.1.2017.
 */
public class DefaultExecutionContext implements ExecutionContext{

    private Storage storage;

    DefaultExecutionContext(Storage storage){
        this.storage = storage;
    }

    @Override
    public Collection getCollection(String name) {
        return storage.getCollection(name);
    }
}
