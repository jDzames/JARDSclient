package net.jards.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The main configuration setup provided for Storage, LocalStorage and
 * RemoteStorage in order optimize execution.
 */
public class StorageSetup {

    private final Map<String, CollectionSetup> localCollections = new HashMap<>();

    private String tablePrefix;
    private String dbAdress; //TODO !!!!!!!!!!!


    public StorageSetup(){

    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void addCollectionSetup(String name, boolean local, String ... indexColumns){
        CollectionSetup collectionSetup = new CollectionSetup(tablePrefix, name, local, indexColumns);
        localCollections.put(name, collectionSetup);
    }

    public void addCollectionSetup(CollectionSetup collectionSetup){
        if (collectionSetup.getTablePrefix() != this.tablePrefix){
            return; //TODO exception here? (wrong refix)
        }
        localCollections.put(collectionSetup.getName(), collectionSetup);
    }

    public Map<String, CollectionSetup> getLocalCollections() {
        return localCollections;
    }
}

/*
*
*
* */