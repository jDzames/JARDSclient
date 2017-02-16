package net.jards.core;

import net.jards.local.sqlite.SqliteException;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class LocalStorage {

    private final String tablePrefix;
    private final Map<String, CollectionSetup> collections;
    private final CollectionSetup setupHashCollection;
    //private final CollectionSetup setupCollection;

	public LocalStorage(StorageSetup storageSetup) {
		this.tablePrefix = storageSetup.getTablePrefix();
        this.collections = storageSetup.getLocalCollections();
        //create hash from storage setup, read hash from special collection (prefix+setupCollection)
        //compare hashes. if same - nothing. if different, update hash and create all collections rom collection setup

        //setupCollection = new CollectionSetup(tablePrefix, "saved_setup_collection", true);
        setupHashCollection = new CollectionSetup(tablePrefix, "setup_hash_table", true);
        int thisSetupHash = computeSetupHash(); //compute hash to be able to compare

        try{
            Map<String, String> savedSetupHashDocument = findOne(null/*query for setup_hash_collection to findOne any*/);
            int savedSetupHash = Integer.parseInt(savedSetupHashDocument.get("jsondata"));
            //compare hashes, if same, done, if different - create new collections (drop those cause prefix)
            if (thisSetupHash != savedSetupHash){
                //fillSetupCollection(); not used
                createCollectionsFromSetup();
            }
        } catch (Exception e) {
            //table does not exist or other error, create new collections, insert into 2 special ones
            //try in method, if error - throw error, something wrong
            //fillSetupCollection(); not used
            createCollectionsFromSetup();
        }
    }

    /**
     * @return hashCode() value of collections from setup
     */
    private int computeSetupHash() {
        return collections.hashCode();
    }

    /**
     * Make documents from collections and insert them into special setupCollection.
     * (Not used, cause collections are hold in map here)
     */
    private void fillSetupCollection() {

    }

    /**
     * Local db uses this setup for the first time or user has changed it.
     * Drop old collections, add new. Also setup hash collection.
     */
    private void createCollectionsFromSetup() {

    }

    /**
     * TODO check if collections from storageSetup exists, if no - create them
     * Starts LocalStorage, if you want to continue, read work that has not been saved (unconfirmed changes)
     * and return it. Storage will use it.
     * @return List of saved requests
     */
    public abstract List<ExecutionRequest> start();

    /**
     * TODO save state - all from queue from thread (unconfirmed requests)
     * Stops the execution, saves changes which have been done, but not confirmed by server yet and not written into database.
     * @param unconfirmedRequests queue of unconfirmed requests
     */
    public abstract void stop(Queue<ExecutionRequest> unconfirmedRequests);

    public abstract void connectDB() throws SqliteException;

    public abstract void addCollection(CollectionSetup collection) throws SqliteException;

    public abstract void removeCollection(String collection) throws SqliteException;

    public abstract String insert(String collectionName, Document document) throws SqliteException;

    public abstract String update(String collectionName, Document document) throws SqliteException;

    public abstract boolean remove(String collectionName, Document document) throws SqliteException;

    public abstract List<Map<String, String>> find(Query query) throws SqliteException;

    public abstract Map<String, String> findOne(Query query) throws SqliteException;

    public String getTablePrefix() {
        return tablePrefix;
    }
}

/*
*
*
* */