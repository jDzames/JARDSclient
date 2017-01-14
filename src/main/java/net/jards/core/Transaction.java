package net.jards.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Transaction {

	private final Storage storage;

	private final Map<String, Document> localChanges = new HashMap<>();

	Transaction(Storage storage) {
		this.storage = storage;
	}

	/*
	 * Package protected vs. protected?
	 */
	
	Document insert(Collection collection, Document document) {
		// storage -> localStorage -> sql/string/... ale nemenim databazu (iba ak je to local?)
		String jsonData = document.getJsonData();
		document = new Document(collection, UUID.randomUUID());
		document.setJsonData(jsonData);
		localChanges.put(document.getUuid().toString(), document);
		return null;
	}
	
	Document update(Collection collection, Document document) {
		return null;
	}
	
	boolean remove(Collection collection, Document document) {
		return false;
	}


	boolean checkIfInThreadForDBRuns() {
		return storage.sameAsThreadForLocalDBRuns(Thread.currentThread());
	}

}

/*
* cim ine od collection metod? - tym ze tu mam Storage -> tade to do vlakna sa posle na vykonanie
*
* */