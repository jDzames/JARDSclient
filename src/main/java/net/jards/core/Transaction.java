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
		// storage -> localStorage -> sql/other.. createDocument

        // createDocument document
        String jsonData = document.getContent();
        document = new Document(collection, idGenerator.getId());
        document.setContent(jsonData);

        //add to local changes
        localChanges.addDocument(document);

        // whats situation...
        if (local){
            // only write, no synchronization, changes to update cursors
            // only to local collection allowed!
            if (!collection.isLocal()){
                //TODO throw new LocalStorageException();
            }
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.createDocument(collection.getName(), document);
        } else if (speculation){
            // only speculation, just put document into changes (done)

        } else {
            // execute - write to db, and send changes to server (put document into changes)
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.createDocument(collection.getName(), document);
        }

		return document;
	}
	
	Document update(Collection collection, Document document) throws LocalStorageException {
        // updating document

        // add to updated documents
        localChanges.updateDocument(document);

        // whats situation...
        if (local){
            // only write, no synchronization, changes to update cursors
            // only to local collection allowed!
            if (!collection.isLocal()){
                //TODO throw new LocalStorageException();
            }
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.updateDocument(collection.getName(), document);
        } else if (speculation){
            // only speculation, just put document into changes (done)
        } else {
            // execute - write to db, and send changes to server
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.updateDocument(collection.getName(), document);
        }

        return document;
	}
	
	boolean remove(Collection collection, Document document) throws LocalStorageException {
        // removing document

        //add to removed documents
        localChanges.addRemovedDocument(document);

        // whats situation...
        if (local){
            // only write, no synchronization, changes only to update cursors
            // only to local collection allowed!
            if (!collection.isLocal()){
                //TODO throw new LocalStorageException();
            }
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.removeDocument(collection.getName(), document);
        } else if (speculation){
            // only speculation, just put document into changes (done)
        } else {
            // execute - write to db, and send changes to server (document in changes)
            LocalStorage localStorage = storage.getLocalStorage();
            localStorage.removeDocument(collection.getName(), document);
        }

        return true;
	}

    DocumentChanges getLocalChanges() {
        return localChanges;
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