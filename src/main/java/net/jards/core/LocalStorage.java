package net.jards.core;

import net.jards.core.Predicate.And;
import net.jards.core.Predicate.Or;
import net.jards.errors.LocalStorageException;
import net.jards.local.sqlite.SqliteException;

import java.util.*;

/**
 * Class for local storage. Serves as interface for communication with local database (or other storage of data).
 * Extend this if you want your own implementation.
 */
public abstract class LocalStorage {

    /**
     * Interface to filter predicates that can be used natively in exact LocalStorage implementation.
     */
    public interface PredicateFilter {
        /**
         * Method to filter predicates
         * @param predicate given predicate
         * @return true if you want to accept this predicate, else false
         */
        boolean isAcceptable(Predicate predicate);
	}

    /**
     * prefix of this user (local storage)
     */
    private final String prefix;
    /**
     * collections used by this storage (name of collection as key, CollectionSetup with settings as value)
     */
    private final LinkedHashMap<String, CollectionSetup> collections;
    /**
     * system collection with hash of this setup
     */
    private final CollectionSetup setupHashCollection;
    /**
     * hash of this setup
     */
    private final String setupHash;
    // private final CollectionSetup setupCollection;
    /**
     * property extractor to extract values from JSON fields
     */
	private final JSONPropertyExtractor jsonPropertyExtractor;

    /**
     * Public constructor for Local storage which prepares all settings for local storage
     * @param storageSetup setup specified by user, need at least prefix in it for system to work
     */
    public LocalStorage(StorageSetup storageSetup) {
        if (storageSetup==null || storageSetup.getPrefix()==null || "".equals(storageSetup.getPrefix())){
            throw new IllegalArgumentException("Specify StorageSetup with prefix!");
        }
		this.jsonPropertyExtractor = storageSetup.getJsonPropertyExtractor();
		this.prefix = storageSetup.getPrefix();
		this.collections = storageSetup.getLocalCollections();
		// createDocument hash from storage setup, read hash from special
		// collection (prefix+setupCollection)
		// compare hashes. if same - nothing. if different, updateDocument hash
		// and createDocument all collections rom collection setup

		// setupCollection = new CollectionSetup(prefix,
		// "saved_setup_collection", true);
		setupHashCollection = new CollectionSetup(prefix, "setup_hash_table", true);
		collections.put("setup_hash_table", setupHashCollection);
		// compute hash to be able to compare
		setupHash = computeSetupHash();
	}

	/**
     * Computes this setup hash and returns value.
	 * @return hash value of collections from setup (just summed strings in this version)
	 */
	private String computeSetupHash() {
        StringBuilder hash = new StringBuilder();
        hash.append(prefix);
        for (CollectionSetup setup:collections.values()){
            hash.append(setup.getName()).append(setup.isLocal());
            for (String index:setup.getOrderedIndexes()){
                hash.append(index).append(setup.getIndexes().get(index));
            }
        }
		return hash.toString();
	}

	/**
	 * Make documents from collections and createDocument them into special
	 * setupCollection. (Not used in this version, cause collections are hold in map as of now)
	 */
	private void fillSetupCollections() {

	}

	/**
	 * Local db uses this setup for the first time or user has changed it. Drop
	 * old collections, add new. Also setup hash collection.
	 */
	private void createCollectionsFromSetup() throws LocalStorageException {
		try {
			connectDB();

			for (CollectionSetup collection : collections.values()) {
				this.removeCollection(collection);
                this.addCollection(collection);
			}
			Collection hashCollection = new Collection(setupHashCollection.getName(), true, null);
			Document hashDocument = new Document(hashCollection, "0");
			hashDocument.setContent("" + setupHash);
			this.removeCollection(setupHashCollection);
			this.addCollection(setupHashCollection);
			this.createDocument(setupHashCollection.getName(), hashDocument);
		} catch (SqliteException e) {
			//e.printStackTrace();
			throw e;
		}

	}

    /**
     * Adds CollectionSetup to this LocalStorage. Possible only if server sends documents with new collection.
     * @param collectionSetup setup of new collection
     */
    protected void addCollectionSetup(CollectionSetup collectionSetup) {
		this.collections.put(collectionSetup.getName(), collectionSetup);
	}

    /**
     * Returns setup for specified collection so implementations of LocalStorage can use it.
     * @param collectionName specified name
     * @return setup for specified collection
     */
    protected CollectionSetup getCollectionSetup(String collectionName) {
		if (!collections.containsKey(collectionName)) {
			return null;
		}
		return collections.get(collectionName);
	}

    /**
     * Invalidates all collections that are remote. Can be used after new subscribe call
     * (some servers send all data after subscribe).
     * @throws LocalStorageException if error happens while dropping or creating collections in database
     */
    protected void invalidateRemoteCollections() throws LocalStorageException {
        for (CollectionSetup collectionSetup:this.collections.values()){
            if (!collectionSetup.isLocal()){
                this.removeCollection(collectionSetup);
                this.addCollection(collectionSetup);
            }
        }
    }

    /**
     * @return extractor used in this class
     */
    protected JSONPropertyExtractor getJsonPropertyExtractor() {
		return jsonPropertyExtractor;
	}

	/**
	 * Checks if collections from storageSetup exists, if no - creates them.
	 * Starts LocalStorage, if you want to continue, read work that has not been
	 * saved (unconfirmed/pending requests) and return it. Storage will use it.
	 * (Using returned requests not implemented yet).
     *
	 * @return List of saved requests
	 */
	List<ExecutionRequest> start() throws LocalStorageException {
		try {
            /* query for setup_hash_collection to findOne any */
			Map<String, String> savedSetupHashDocument = findOne(setupHashCollection.getName(), null, null);
			String savedSetupHash = savedSetupHashDocument.get("jsondata");
			// compare hashes, if same, done, if different - create new
			// collections (drop old cause of same prefix)
            //System.out.println("hashes: "+setupHash+" a "+savedSetupHash);
            if (!setupHash.equals(savedSetupHash)) {
				// fillSetupCollections(); not used
				createCollectionsFromSetup();
			}
		} catch (Exception e) {
			// table does not exist or other error, createDocument new
			// collections, createDocument into 2 special ones
			// try in method, if error - throw error, something wrong
			// fillSetupCollections(); not used
			createCollectionsFromSetup();
		}

		// TODO read unfinished work?
		List<ExecutionRequest> requests = startLocalStorage();
		return requests;
	}

	/**
	 * Stops the execution, saves changes which have been done, but not confirmed by
	 * server yet and not written into database.
     * (Saving changes not implemented yet. What should be saved (which queues with work)? All?)
	 * 
	 * @param unconfirmedRequests queue of unconfirmed requests
	 */
	void stop(Queue<ExecutionRequest> unconfirmedRequests) {
		// TODO save unfinished work???
		stopLocalStorage(unconfirmedRequests);
	}

    /**
     * Starts local storage and reads saved requests.
     * (Using returned requests not implemented yet).
     * @return List of execution request.
     */
    protected abstract List<ExecutionRequest> startLocalStorage();

    /**
     * Extend to stop LocalStorage and saves requests.
     * (Requests work not implemented yet.)
     * @param unconfirmedRequests requests to save (pending requests should be added too probably)
     */
    protected abstract void stopLocalStorage(Queue<ExecutionRequest> unconfirmedRequests);

    /**
     * Connect to local database
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract void connectDB() throws LocalStorageException;

    /**
     * Extend to add collection to database
     * @param collection collection to add
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract void addCollection(CollectionSetup collection) throws LocalStorageException;

	/* we use invalidate, not this.
	void removeCollection(String collectionName) throws LocalStorageException {
        if (collections.containsKey(collectionName)){
            this.removeCollection(collections.get(collectionName));
        }
	}*/

    /**
     * Extend to remove collection
     * @param collection collection to remove
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract void removeCollection(CollectionSetup collection) throws LocalStorageException;

    /**
     * Extend to create document and write it into database.
     * @param collectionName name of collection for document
     * @param document document to create write to database)
     * @return string representation of id of created document
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract String createDocument(String collectionName, Document document) throws LocalStorageException;

    /**
     * Extend to update selected document.
     * @param collectionName name of collection to which document belongs
     * @param document document you want to update (original id, changed content)
     * @return string representation of id of updated document
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract String updateDocument(String collectionName, Document document) throws LocalStorageException;

    /**
     * Extend to remove selected document
     * @param collectionName name of collection where selected document belongs
     * @param document document to remove
     * @return true if document was removed successfully
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract boolean removeDocument(String collectionName, Document document) throws LocalStorageException;

	/**
	 * Applies changes to local database. Creates new collections if needed,
	 * create new documents, updates edited and removed deleted.
	 * 
	 * @param remoteDocumentChanges List of DocumentChanges. Can contain collections which does
	 *            not exist in local database.
	 * @throws LocalStorageException throws exception if any of write updates fails
	 */
	protected abstract void applyDocumentChanges(DocumentChanges remoteDocumentChanges)
			throws LocalStorageException;

    /**
     * Extend to find selected documents and return them in list (in map representation with id,
     * collection and content).
     * Collection then creates documents from it and put them into ResultSet.
     * @param collectionName name of collection
     * @param p predicate to filter result
     * @param options options for result (ie. order...)
     * @return list with documents in map representation (with id, collection and content)
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract List<Map<String, String>> find(String collectionName, Predicate p, ResultOptions options) throws LocalStorageException;

    /**
     * Extend to find selected document (you can find all matching predicate and return first from result).
     * @param collectionName name of collection
     * @param p predicate to filter result
     * @param options options for result (ie. order...)
     * @return map representation of found document
     * @throws LocalStorageException if error happens while working with local database
     */
    protected abstract Map<String, String> findOne(String collectionName, Predicate p, ResultOptions options) throws LocalStorageException;

    /**
     * @return prefix of this storage (user)
     */
    protected String getPrefix() {
		return prefix;
	}

    /**
     * You can extend and return true if you support selected predicate
     * @param predicate predicate asked about being supported
     * @return true if local storage supports selected predicate natively, else false
     */
    public boolean hasNativeSupportForPredicate(Predicate predicate) {
		return false;
	}

	/**
	 * Helper method for local storage implementations that returns a filtering
	 * predicate formed by predicates natively supported by the local storage.
	 * 
	 * @param predicate the predicate.
	 * @param filter the predicate filter.
	 * @return
	 */
	protected static Predicate createFilteringPredicate(Predicate predicate, PredicateFilter filter) {
		if (predicate == null) {
			return null;
		}

		if (!filter.isAcceptable(predicate)) {
			return null;
		}

		if (predicate instanceof And) {
			List<Predicate> predicates = new ArrayList<>();
			for (Predicate p : ((And) predicate).getSubPredicates()) {
				Predicate filteredPredicate = createFilteringPredicate(p, filter);
				if (filteredPredicate != null) {
					predicates.add(filteredPredicate);
				}
			}

			if (predicates.isEmpty()) {
				return null;
			}

			if (predicates.size() == 1) {
				return predicates.get(0);
			}

			return new And(predicates.toArray(new Predicate[predicates.size()]));
		}

		if (predicate instanceof Or) {
			List<Predicate> predicates = new ArrayList<>();
			for (Predicate p : ((Or) predicate).getSubPredicates()) {
				Predicate filteredPredicate = createFilteringPredicate(p, filter);
				if (filteredPredicate == null) {
					return null;
				}
			}

			if (predicates.isEmpty()) {
				return null;
			}

			if (predicates.size() == 1) {
				return predicates.get(0);
			}

			return new Or(predicates.toArray(new Predicate[predicates.size()]));
		}

		//equals, compare, equal properties, compare properties not needed, checked by filter (if there is index for properties)

		return predicate;
	}
}
