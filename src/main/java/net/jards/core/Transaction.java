package net.jards.core;

import java.util.HashMap;
import java.util.Map;

public class Transaction {

	private final Storage storage;

    private final IdGenerator idGenerator;

	private final Map<String, Document> localChanges = new HashMap<>();

	Transaction(Storage storage, IdGenerator idGenerator) {
		this.storage = storage;
        this.idGenerator = idGenerator;
	}

	/*
	 * Package protected vs. protected?
	 */
	
	Document insert(Collection collection, Document document) {
		// storage -> localStorage -> sql/string/... ale nemenim databazu (iba ak je to local?)
		String jsonData = document.getJsonData();
		document = new Document(collection, idGenerator.getId());
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

    Storage getStorage() {
        return storage;
    }

    boolean checkIfInThreadForDBRuns() {
		return storage.sameAsThreadForLocalDBRuns(Thread.currentThread());
	}

}

/*
* cim ine od collection metod? - tym ze tu mam Storage -> tade to do vlakna sa posle na vykonanie
*
* */