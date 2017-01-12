package net.jards.core;

public class Transaction {

	private final Storage storage;

	Transaction(Storage storage) {
		this.storage = storage;
	}

	/*
	 * Package protected vs. protected?
	 */
	
	Document insert(Collection collection, Document document) {
		// storage -> localStorage -> sql/string/...
		return null;
	}
	
	Document update(Collection collection, Document document) {
		return null;
	}
	
	boolean remove(Collection collection, Document document) {
		return false;
	}


	public boolean checkIfInThreadForDBRuns() {
		return storage.sameAsThreadForLocalDBRuns(Thread.currentThread());
	}

}

/*
* cim ine od collection metod? - tym ze tu mam Storage -> tade to do vlakna sa posle na vykonanie
*
* */