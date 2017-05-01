package net.jards.core;

import net.jards.errors.LocalStorageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class representing collection - structure which holds and manipulates with documents. Alternative to SQL table.
 */
public class Collection {

	/**
	 * Storage that manages the collection.
	 */
	private final Storage storage;

	/**
	 * Name/identifier of the collection.
	 */
	private final String name;

	/**
	 * Indicates whether the collection is local.
	 */
	private final boolean local;

    /**
	 * Package protected constructor of collection.
	 *
     * @param name the name of collection.
     * @param local the indicator whether the collection is local.
     * @param storage storage which uses this collection
     */
	Collection(String name, boolean local, Storage storage) {
		this.name = name;
		this.storage = storage;
		this.local = local;
	}

    /**
     * Package protected constructor of collection with CollectionSetup.
     * @param collectionSetup setup for new created collection
     * @param storage storage reference
     */
    Collection(CollectionSetup collectionSetup, Storage storage){
        this.name = collectionSetup.getName();
        this.storage = storage;
        this.local = collectionSetup.isLocal();
    }

    /**
     * Method used to create document.
     * @param document document with content which you want to save
     * @param transaction transaction given in run method of TransactionRunnable
     * @return new created document with id, collection and content from original document
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
    public Document create(Document document, Transaction transaction) throws LocalStorageException {
		checkTransaction(transaction);
		return transaction.create(this, document);
	}

    /**
     * Method for removing documents.
     * @param document document you want to remove
     * @param transaction transaction given in run method of TransactionRunnable
     * @return true if document was removed successfully, false else
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
    public boolean remove(Document document, Transaction transaction) throws LocalStorageException  {
		checkTransaction(transaction);
		return transaction.remove(this, document);
	}

    /**
     * Method for updating documents.
     * @param document document you want to update (with changes)
     * @param transaction transaction given in run method of TransactionRunnable
     * @return updated document
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
    public Document update(Document document, Transaction transaction) throws LocalStorageException  {
		checkTransaction(transaction);
		return transaction.update(this, document);
	}

    /**
     * Method that checks transaction if it's correct.
     * @param transaction transaction you want to check
     */
    private void checkTransaction(Transaction transaction) {
		// check if transaction is ok - i.e. it has not been closed
		// also if we are in right thread
		if (transaction == null){
			throw new NullPointerException("Transaction can't be null!");
		}
		boolean rightThread = transaction.checkIfInThreadForDBRuns();
		if (!rightThread){
            throw new IllegalStateException("Incorrect way to execute transaction! It should be executed ");
		}

	}

    /**
     * Finds all documents for this collection.
     * @return ResultSet object with documents
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
    public ResultSet find() throws LocalStorageException {
        return this.find(null, null);
    }

    /**
     * Finds all documents for this collection matching selected predicate.
     * @param predicate predicate filtering result
     * @return ResultSet object with documents
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
	public ResultSet find(Predicate predicate) throws LocalStorageException {
		return this.find(predicate, null);
	}

    /**
     * Finds all documents for this collection matching selected predicate. Sets documents by specified options.
     * @param predicate predicate filtering result
     * @param resultOptions options for result (ie. order...)
     * @return ResultSet object with documents
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
    public ResultSet find(Predicate predicate, ResultOptions resultOptions) throws LocalStorageException {
        //execute query
        LocalStorage localStorage = storage.getLocalStorage();
        List<Map<String, String>> result = localStorage.find(getName(), predicate, resultOptions);
        //create result set
        if (result == null){
            return null;
        }
        List<Document> originalQueryDocuments = new ArrayList<>();
        for (Map<String, String> docMap:result) {
            originalQueryDocuments.add(new Document(docMap, storage));
        }
        ResultSet resultSet = new ResultSet(predicate, this, resultOptions);

        //add result set to opened result sets in storage
        storage.addOpenedResultSet(resultSet);

        //add initial data
        resultSet.setResult(originalQueryDocuments);

        return resultSet;
    }

    /**
     * Method to find one (first) document from collection.
     * @return first document read from collection
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
    public Document findOne() throws LocalStorageException {
        return this.findOne(null, null);
    }

    /**
     * Method to find one (first) document from collection.
     * @param predicate predicate used to specify desired document
     * @return first document read from collection
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
    public Document findOne(Predicate predicate) throws LocalStorageException {
        return this.findOne(predicate, null);
    }

    /**
     * Method to find one (first) document from collection.
     * @param predicate predicate used to specify desired document
     * @param resultOptions options for result (ie. order...), applied before getting first document
     * @return first document read from collection
     * @throws LocalStorageException if error happens while creating document in local storage (database usually)
     */
	public Document findOne(Predicate predicate, ResultOptions resultOptions) throws LocalStorageException {
        LocalStorage localStorage = storage.getLocalStorage();
        return new Document(localStorage.findOne(getName(), predicate, resultOptions), storage);
    }

	Storage getStorage() {
		return storage;
	}

    /**
     * @return collection name
     */
    public String getName() {
		return name;
	}

    /**
     * @return true if method is local (not synchronized to server), else false
     */
    public boolean isLocal() {
		return local;
	}
}

