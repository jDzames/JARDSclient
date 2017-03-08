package net.jards.core;

import net.jards.errors.LocalStorageException;

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

	Collection(CollectionSetup collectionSetup, Storage storage){
        this.name = collectionSetup.getName();
        this.storage = storage;
        this.local = collectionSetup.isLocal();
    }

	public Document create(Document document, Transaction transaction) throws LocalStorageException {
		checkTransaction(transaction);
		return transaction.insert(this, document);
	}

	public boolean remove(Document document, Transaction transaction) throws LocalStorageException  {
		checkTransaction(transaction);
		return transaction.remove(this, document);
	}

	public Document update(Document document, Transaction transaction) throws LocalStorageException  {
		checkTransaction(transaction);
		return transaction.update(this, document);
	}

	private void checkTransaction(Transaction transaction) {
		// check if transaction is ok - i.e. it has not been closed
		// also if we are in right thread
		if (transaction == null){
			//TODO error?
			return;
		}
		boolean rightThread = transaction.checkIfInThreadForDBRuns();
		if (!rightThread){
			//TODO error?
			return;
		}

	}

	public ResultSet find(Query query, Transaction transaction) {
		// operacie na ziskanie udajov sa vykonavaju asynchronne - nie su teda
		// viazane na transakciu.
		return null;
	}

	public Document findOne(Query query, Transaction transaction){

        return null;
    }

	Storage getStorage() {
		return storage;
	}

	public String getName() {
		return name;
	}

	public boolean isLocal() {
		return local;
	}
}



/*
* cez transaction, lebo to ma storage
*
* */