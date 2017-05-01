package net.jards.core;

import net.jards.errors.LocalStorageException;

/**
 * Class representing transaction. Transaction executes document operations and holds document changes.
 * This way it creates overlays.
 */
public class Transaction {

    /**
     * storage reference
     */
    private final Storage storage;

    /**
     * true if transaction is meant for local execution
     */
    private boolean local;
    /**
     * true if this transaction execution should be speculation
     */
    private boolean speculation;

    /**
     * id generator reference
     */
    private final IdGenerator idGenerator;

    /**
     * local changes made by this transaction (overlay)
     */
    private final DocumentChanges localChanges = new DocumentChanges();

    /**
     * Package protected constructor for transaction.
     * @param storage reference to storage
     * @param idGenerator id generator reference
     */
    Transaction(Storage storage, IdGenerator idGenerator) {
		this.storage = storage;
        this.idGenerator = idGenerator;
	}

    /**
     * Method that creates document (gets document with content only and creates full functioning document).
     * Writes changes to database (if execute) or adds document to overlay (if speculation).
     * @param collection collection where document belongs
     * @param document document (with content only)
     * @return new created document with id and collection
     * @throws LocalStorageException thrown if problems happens while writing to database
     */
    Document create(Collection collection, Document document) throws LocalStorageException {
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

    /**
     * Method that updates document (from document with updated content).
     * Writes changes to database (if execute) or adds document to overlay (if speculation).
     * @param collection collection where document belongs
     * @param document document (with updated content)
     * @return updated document
     * @throws LocalStorageException thrown if problems happens while writing to database
     */
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

    /**
     * Method that removes selected document.
     * Writes changes to database (if execute) or adds document to overlay (if speculation).
     * @param collection collection where document belongs
     * @param document document (with updated content)
     * @return true if deletion finished successfully
     * @throws LocalStorageException thrown if problems happens while writing to database
     */
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

    /**
     * @return
     */
    DocumentChanges getLocalChanges() {
        return localChanges;
    }

    /**
     * @return storage given to this transaction
     */
    Storage getStorage() {
        return storage;
    }

    /**
     * @return true if in right thread (for local storage operations)
     */
    boolean checkIfInThreadForDBRuns() {
		return storage.sameAsThreadForLocalDBRuns(Thread.currentThread());
	}

    /**
     * @param speculation speculation code
     */
    void setSpeculation(boolean speculation) {
        this.speculation = speculation;
    }

    /**
     * @param local true if its executeLocally method, else false
     */
    void setLocal(boolean local) {
        this.local = local;
    }
}
