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
     * Prefix for this instance.
     */
    private final String prefix;

    /**
	 * Package protected constructor of collection.
	 *
     * @param prefix prefix for this instance
     * @param name the name of collection.
     * @param local the indicator whether the collection is local.
     * @param storage storage which uses this collection
     */
	Collection(String prefix, String name, boolean local, Storage storage) {
        this.prefix = prefix;
		this.name = name;
		this.storage = storage;
		this.local = local;
	}

	Collection(CollectionSetup collectionSetup, Storage storage){
        this.prefix = collectionSetup.getTablePrefix();
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
		// overime, ci transakcia je ok - napriklad ci uz nebola uzatvorena
		// alebo sme vo vlakne, v ktorom bola vytvorena transakcia
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

	public ResultSet find(Query query) {
		// operacie na ziskanie udajov sa vykonavaju asynchronne - nie su teda
		// viazane na transakciu.
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

    String getFullName() {
        return prefix+name;
    }
}



/*
* cez transaction, lebo to ma storage
*
* */