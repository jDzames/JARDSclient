package net.jards.core;

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
	 * @param name
	 *            the name of collection.
	 * @param local
	 *            the indicator whether the collection is local.
	 * @param storage
	 *            the storage.
	 */
	Collection(String name, boolean local, Storage storage) {
		this.name = name;
		this.storage = storage;
		this.local = local;
	}

	public Document insert(Document document, Transaction transaction) {
		checkTransaction(transaction);
		return transaction.insert(this, document);
	}

	public boolean remove(Document document, Transaction transaction) {
		checkTransaction(transaction);
		return transaction.remove(this, document);
	}

	public Document update(Document document, Transaction transaction) {
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

	public Storage getStorage() {
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