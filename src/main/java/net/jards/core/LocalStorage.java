package net.jards.core;

import net.jards.core.Predicate.And;
import net.jards.core.Predicate.Or;
import net.jards.errors.LocalStorageException;
import net.jards.local.sqlite.SqliteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class LocalStorage {

	public interface PredicateFilter {
		boolean isAcceptable(Predicate predicate);
	}

	private final String prefix;
	private final Map<String, CollectionSetup> collections;
	private final CollectionSetup setupHashCollection;
	private final int setupHash;
	// private final CollectionSetup setupCollection;
	private final JSONPropertyExtractor jsonPropertyExtractor;

	public LocalStorage(StorageSetup storageSetup) throws LocalStorageException {
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
	 * @return hashCode() value of collections from setup
	 */
	private int computeSetupHash() {
		return collections.hashCode();
	}

	/**
	 * Make documents from collections and createDocument them into special
	 * setupCollection. (Not used, cause collections are hold in map here)
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
			hashDocument.setJsonData("" + setupHash);
			this.removeCollection(setupHashCollection);
			this.addCollection(setupHashCollection);
			this.createDocument(setupHashCollection.getName(), hashDocument);
		} catch (SqliteException e) {
			//
			e.printStackTrace();
			throw e;
		}

	}

	protected void addCollectionSetup(CollectionSetup collectionSetup) {
		this.collections.put(collectionSetup.getName(), collectionSetup);
	}

	protected CollectionSetup getCollectionSetup(String collectionName) {
		if (!collections.containsKey(collectionName)) {
			return null;
		}
		return collections.get(collectionName);
	}

	protected JSONPropertyExtractor getJsonPropertyExtractor() {
		return jsonPropertyExtractor;
	}

	/**
	 * Checks if collections from storageSetup exists, if no - creates them.
	 * Starts LocalStorage, if you want to continue, read work that has not been
	 * saved (unconfirmed changes) and return it. Storage will use it.
	 * 
	 * @return List of saved requests
	 */
	List<ExecutionRequest> start() throws LocalStorageException {
		try {
			Map<String, String> savedSetupHashDocument = findOne(
					null/* query for setup_hash_collection to findOne any */);
			int savedSetupHash = Integer.parseInt(savedSetupHashDocument.get("jsondata"));
			// compare hashes, if same, done, if different - createDocument new
			// collections (drop those cause prefix)
			if (setupHash != savedSetupHash) {
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
	 * TODO save state - all from queue from thread (unconfirmed requests) Stops
	 * the execution, saves changes which have been done, but not confirmed by
	 * server yet and not written into database.
	 * 
	 * @param unconfirmedRequests
	 *            queue of unconfirmed requests
	 */
	void stop(Queue<ExecutionRequest> unconfirmedRequests) {
		// TODO save unfinished work???
		stopLocalStorage(unconfirmedRequests);
	}

	protected abstract List<ExecutionRequest> startLocalStorage();

	protected abstract void stopLocalStorage(Queue<ExecutionRequest> unconfirmedRequests);

	protected abstract void connectDB() throws LocalStorageException;

	protected abstract void addCollection(CollectionSetup collection) throws LocalStorageException;

	void removeCollection(String collectionName) throws LocalStorageException {
		this.removeCollection(collections.get(collectionName));
	}

	protected abstract void removeCollection(CollectionSetup collection) throws LocalStorageException;

	protected abstract String createDocument(String collectionName, Document document) throws LocalStorageException;

	protected abstract String updateDocument(String collectionName, Document document) throws LocalStorageException;

	protected abstract boolean removeDocument(String collectionName, Document document) throws LocalStorageException;

	/**
	 * Applies changes to local database. Creates new collections if needed,
	 * insert new documents, updates edited and removed deleted.
	 * 
	 * @param remoteDocumentChanges
	 *            List of DocumentChanges. Can contain collections which does
	 *            not exist in local database.
	 * @throws LocalStorageException
	 *             throws exception if any of write updates fails
	 */
	protected abstract void applyDocumentChanges(List<DocumentChanges> remoteDocumentChanges)
			throws LocalStorageException;

	protected abstract List<Map<String, String>> find(Query query) throws LocalStorageException;

	protected abstract Map<String, String> findOne(Query query) throws LocalStorageException;

	protected String getPrefix() {
		return prefix;
	}

	public boolean hasNativeSupportForPredicate(Predicate predicate) {
		return false;
	}

	/**
	 * Helper method for local storage implementations that returns a filtering
	 * predicate formed by predicates natively supported by the local storage.
	 * 
	 * @param predicate
	 *            the predicate.
	 * @param filter
	 *            the predicate filter.
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

		return predicate;
	}
}

/*
*
*
* */