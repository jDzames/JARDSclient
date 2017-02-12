package net.jards.core;

import net.jards.errors.LocalStorageException;

public class Transaction {

	private final Storage storage;

    private boolean local;
    private boolean speculation;

    private final IdGenerator idGenerator;

	private final DocumentChanges localChanges = new DocumentChanges();

	Transaction(Storage storage, IdGenerator idGenerator) {
		this.storage = storage;
        this.idGenerator = idGenerator;
	}

	/*
	 * Package protected vs. protected?
	 */
	
	Document insert(Collection collection, Document document) throws LocalStorageException {
		// storage -> localStorage -> sql/other.. insert

        // create document
        String jsonData = document.getJsonData();
        document = new Document(collection, idGenerator.getId());
        document.setJsonData(jsonData);

        // whats situation...
        if (local){
            // only write, no synchronization, changes..
            // only to local collection allowed!
            if (!collection.isLocal()){
                //TODO throw new LocalStorageException();
            }
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.insert(collection.getFullName(), document);
        } else if (speculation){
            // only speculation, just put document into changes
            localChanges.addDocument(document);
        } else {
            // execute - write to db, and send changes to server (put document into changes)
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.insert(collection.getFullName(), document);
            localChanges.addDocument(document);
        }

		return document;
	}
	
	Document update(Collection collection, Document document) throws LocalStorageException {
        // updating document

        // whats situation...
        if (local){
            // only write, no synchronization, changes..
            // only to local collection allowed!
            if (!collection.isLocal()){
                //TODO throw new LocalStorageException();
            }
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.update(collection.getFullName(), document);
        } else if (speculation){
            // only speculation, just put document into changes
            localChanges.updateDocument(document);
        } else {
            // execute - write to db, and send changes to server
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.update(collection.getFullName(), document);
            localChanges.updateDocument(document);
        }

        return document;
	}
	
	boolean remove(Collection collection, Document document) throws LocalStorageException {
        // removing document

        // whats situation...
        if (local){
            // only write, no synchronization, changes..
            // only to local collection allowed!
            if (!collection.isLocal()){
                //TODO throw new LocalStorageException();
            }
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.remove(collection.getFullName(), document);
        } else if (speculation){
            // only speculation, just put document into changes
            localChanges.removeDocument(document);
        } else {
            // execute - write to db, and send changes to server (document into changes)
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.remove(collection.getFullName(), document);
            localChanges.removeDocument(document);
        }

        return true;
	}

    Storage getStorage() {
        return storage;
    }

    boolean checkIfInThreadForDBRuns() {
		return storage.sameAsThreadForLocalDBRuns(Thread.currentThread());
	}

    void setSpeculation(boolean speculation) {
        this.speculation = speculation;
    }

    void setLocal(boolean local) {
        this.local = local;
    }
}

/*
*
*
* */