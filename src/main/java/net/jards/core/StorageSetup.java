package net.jards.core;

import java.util.LinkedHashMap;

/**
 * The main configuration setup provided for Storage, LocalStorage and
 * RemoteStorage in order optimize execution.
 */
public class StorageSetup {

    /**
     * Enum for remote login strategy: no login, login if it's possible, demand login.
     */
    public enum RemoteLoginType {
        NoLogin,
        LoginIfPossible,
        DemandLogin
    }

    /**
     * map with setup for local collections
     */
    private final LinkedHashMap<String, CollectionSetup> localCollections = new LinkedHashMap<>();

    /**
     * prefix for this user
     */
    private String prefix;
    /**
     * json property extractor (user can specify his own)
     */
    private JSONPropertyExtractor jsonPropertyExtractor = null;
    /**
     * remote login type/strategy
     */
    private RemoteLoginType remoteLoginType;

    /**
     * Constructor, sets default values.
     */
    public StorageSetup(){
        jsonPropertyExtractor = new DefaultJSONPropertyExtractor();
        remoteLoginType = RemoteLoginType.NoLogin;
    }

    /**
     * @param prefix prefix for this user
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        for (CollectionSetup collectionSetup:localCollections.values()) {
            collectionSetup.setPrefix(prefix);
        }
    }

    /**
     * @return prefix of this user
     */
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

    /**
     * Creates and adds collection (adds simple collection setup without indexes).
     * @param name name of collection
     */
    public void addCollection(String name){
        CollectionSetup collectionSetup = new CollectionSetup(prefix, name, false);
        localCollections.put(name, collectionSetup);
    }

    /**
     * Adds collection setup.
     * @param collectionSetup collection setup that will be added
     */
    public void addCollectionSetup(CollectionSetup collectionSetup){
        if (collectionSetup.getPrefix().equals(this.prefix)){
            throw new IllegalArgumentException("You have to specify same prefix as for StorageSetup!");
        }
        localCollections.put(collectionSetup.getName(), collectionSetup);
    }

    /**
     * @return map of local collections
     */
    public LinkedHashMap<String, CollectionSetup> getLocalCollections() {
        return localCollections;
    }

    /**
     * @param jsonPropertyExtractor specify your preferred json property extractor
     */
    public void setJsonPropertyExtractor(JSONPropertyExtractor jsonPropertyExtractor) {
        this.jsonPropertyExtractor = jsonPropertyExtractor;
    }

    /**
     * @param remoteLoginType set preferred login type/strategy
     */
    public void setRemoteLoginType(RemoteLoginType remoteLoginType) {
        this.remoteLoginType = remoteLoginType;
    }

    /**
     * @return selected json property extractor
     */
    public JSONPropertyExtractor getJsonPropertyExtractor() {
        return jsonPropertyExtractor;
    }

    /**
     * @return selected remote login type/strategy
     */
    public RemoteLoginType getRemoteLoginType() {
        return remoteLoginType;
    }
}
