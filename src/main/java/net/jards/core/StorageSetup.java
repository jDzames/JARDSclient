package net.jards.core;

import java.util.HashMap;
import java.util.Map;

/**
 * The main configuration setup provided for Storage, LocalStorage and
 * RemoteStorage in order optimize execution.
 */
public class StorageSetup {

    private final Map<String, CollectionSetup> localCollections = new HashMap<>();

    private String prefix;
    private String dbAddress;
    private JSONPropertyExtractor jsonPropertyExtractor = null;


    public StorageSetup(){

    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        for (CollectionSetup collectionSetup:localCollections.values()) {
            collectionSetup.setPrefix(prefix);
        }
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Creates and adds collection setup
     * @param name name of collection
     * @param local true if collection is local only, else false
     * @param indexColumns any numbers of indexes (defined as path to value in json)
     */
    public void addCollectionSetup(String name, boolean local, String ... indexColumns){
        CollectionSetup collectionSetup = new CollectionSetup(prefix, name, local, indexColumns);
        localCollections.put(name, collectionSetup);
    }

    public void addCollectionSetup(CollectionSetup collectionSetup){
        if (collectionSetup.getPrefix() != this.prefix){
            return; //TODO exception here? (wrong prefix)
        }
        localCollections.put(collectionSetup.getName(), collectionSetup);
    }

    public Map<String, CollectionSetup> getLocalCollections() {
        return localCollections;
    }

    public void setJsonPropertyExtractor(JSONPropertyExtractor jsonPropertyExtractor) {
        this.jsonPropertyExtractor = jsonPropertyExtractor;
    }

    public void setDbAddress(String dbAddress) {
        this.dbAddress = dbAddress;
    }

    public JSONPropertyExtractor getJsonPropertyExtractor() {
        if (jsonPropertyExtractor == null ){
            return new DefaultJSONPropertyExtractor();
        }
        return jsonPropertyExtractor;
    }

    public String getDbAdress() {
        return dbAddress;
    }
}

/*
*
*
* */