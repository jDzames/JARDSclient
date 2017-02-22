package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.local.sqlite.SqliteException;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class LocalStorage {

    private final String tablePrefix;
    private final Map<String, CollectionSetup> collections;
    private final CollectionSetup setupHashCollection;
    private final int setupHash;
    //private final CollectionSetup setupCollection;
    private final JSONPropertyExtractor jsonPropertyExtractor;

	public LocalStorage(StorageSetup storageSetup) throws LocalStorageException {
        this.jsonPropertyExtractor = storageSetup.getJsonPropertyExtractor();
		this.tablePrefix = storageSetup.getTablePrefix();
        this.collections = storageSetup.getLocalCollections();
        //createDocument hash from storage setup, read hash from special collection (prefix+setupCollection)
        //compare hashes. if same - nothing. if different, updateDocument hash and createDocument all collections rom collection setup

        //setupCollection = new CollectionSetup(tablePrefix, "saved_setup_collection", true);
        setupHashCollection = new CollectionSetup(tablePrefix, "setup_hash_table", true);
        collections.put("setup_hash_table", setupHashCollection);
        //compute hash to be able to compare
        setupHash = computeSetupHash();

        try{
            Map<String, String> savedSetupHashDocument = findOne(null/*query for setup_hash_collection to findOne any*/);
            int savedSetupHash = Integer.parseInt(savedSetupHashDocument.get("jsondata"));
            //compare hashes, if same, done, if different - createDocument new collections (drop those cause prefix)
            if (setupHash != savedSetupHash){
                //fillSetupCollections(); not used
                createCollectionsFromSetup();
            }
        } catch (Exception e) {
            //table does not exist or other error, createDocument new collections, createDocument into 2 special ones
            //try in method, if error - throw error, something wrong
            //fillSetupCollections(); not used
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
     * Make documents from collections and createDocument them into special setupCollection.
     * (Not used, cause collections are hold in map here)
     */
    private void fillSetupCollections() {

    }

    /**
     * Local db uses this setup for the first time or user has changed it.
     * Drop old collections, add new. Also setup hash collection.
     */
    private void createCollectionsFromSetup() throws LocalStorageException{
        try {
            connectDB();

            for (CollectionSetup collection:collections.values()) {
                this.removeCollection(collection);
                this.addCollection(collection);
            }
            Collection hashCollection = new Collection(tablePrefix, setupHashCollection.getName(), true, null);
            Document hashDocument = new Document(hashCollection, "0");
            hashDocument.setJsonData(""+setupHash);
            System.out.println("before remove");
            this.removeCollection(setupHashCollection);
            System.out.println("before adding collection");
            this.addCollection(setupHashCollection);
            System.out.println("before adding document "+setupHashCollection.getName());
            this.createDocument(setupHashCollection.getName(), hashDocument);
        } catch (SqliteException e) {
            throw new SqliteException(SqliteException.SETUP_EXCEPTION,
                    "LocalStorage, creating collections from setup",
                    "Problem creating collections in LocalStorage. "+e.toString());
        }

    }

    protected CollectionSetup getCollectionSetup(String collectionName){
        return  collections.get(collectionName);
    }

    protected JSONPropertyExtractor getJsonPropertyExtractor() {
        return jsonPropertyExtractor;
    }

    /**
     * TODO check if collections from storageSetup exists, if no - createDocument them
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

    public abstract void connectDB() throws LocalStorageException;

    public abstract void addCollection(CollectionSetup collection) throws LocalStorageException;

    void removeCollection(String collectionName) throws LocalStorageException {
        this.removeCollection(collections.get(collectionName));
    }

    public abstract void removeCollection(CollectionSetup collection) throws LocalStorageException;

    public abstract String createDocument(String collectionName, Document document) throws LocalStorageException;

    public abstract String updateDocument(String collectionName, Document document) throws LocalStorageException;

    public abstract boolean removeDocument(String collectionName, Document document) throws LocalStorageException;

    public abstract void applyDocumentChanges(List<DocumentChanges> remoteDocumentChanges) throws LocalStorageException;

    public abstract List<Map<String, String>> find(Query query) throws LocalStorageException;

    public abstract Map<String, String> findOne(Query query) throws LocalStorageException;

    public String getTablePrefix() {
        return tablePrefix;
    }
}

/*
*
*
* */