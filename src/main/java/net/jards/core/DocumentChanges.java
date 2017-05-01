package net.jards.core;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Class representing changes done in one operation.
 */
public class DocumentChanges {

    /**
     * Added documents in operation
     */
    private LinkedHashMap<String, Document> addedDocuments;
    /**
     * Updated documents in operation
     */
    private LinkedHashMap<String, Document> updatedDocuments;
    /**
     * Removed documents in operation
     */
    private LinkedHashMap<String, Document> removedDocuments;

    /**
     * Constructor for DocumentChanges that creates empty lists with changes.
     */
    public DocumentChanges(){
        addedDocuments = new LinkedHashMap<>();
        updatedDocuments = new LinkedHashMap<>();
        removedDocuments = new LinkedHashMap<>();
    }

    /**
     * @return documents added in corresponding operation
     */
    public List<Document> getAddedDocuments() {
        List<Document> list = new LinkedList<>();
        list.addAll(addedDocuments.values());
        return list;
    }

    /**
     * @return documents updated in corresponding operation
     */
    public List<Document> getUpdatedDocuments() {
        List<Document> list = new LinkedList<>();
        list.addAll(updatedDocuments.values());
        return list;
	}

    /**
     * @return documents removed in corresponding operation
     */
    public List<Document> getRemovedDocuments() {
        List<Document> list = new LinkedList<>();
        list.addAll(removedDocuments.values());
        return list;
	}

    /**
     * @param addedDocuments list of documents to add to added documents in this changes object
     */
    void addDocuments(List<Document> addedDocuments){
        for (Document doc:addedDocuments) {
            this.addedDocuments.put(doc.getId(), doc);
        }
    }

    /**
     * @param updatedDocuments list of documents to add to updated documents in this changes object
     */
    void updateDocuments(List<Document> updatedDocuments){
        for (Document doc:updatedDocuments) {
            this.updatedDocuments.put(doc.getId(), doc);
        }
    }

    /**
     * @param removedDocuments list of documents to add to removed documents in this changes object
     */
    void addRemovedDocuments(List<Document> removedDocuments){
        for (Document doc:removedDocuments) {
            this.removedDocuments.put(doc.getId(), doc);
        }
    }

    /**
     * @param document document to add to added documents
     */
    void addDocument(Document document){
        this.addedDocuments.put(document.getId(), document);
    }

    /**
     * @param document document to add to updated documents
     */
    void updateDocument(Document document){
        this.updatedDocuments.put(document.getId(), document);
    }

    /**
     * @param document document to add to removed documents
     */
    void addRemovedDocument(Document document){
        this.removedDocuments.put(document.getId(), document);
    }

    /**
     * Removes specified document from these changes.
     * Used to remove changes from overlay when I receive data (that document) from server.
     * (More reactive result than remove whole overlay after confirmation. Testing such use. )
     * @param document document to remove from this changes object
     */
    void removeDocumentFromChanges(Document document){
        this.addedDocuments.remove(document.getId());
        this.updatedDocuments.remove(document.getId());
        this.removedDocuments.remove(document.getId());
    }

    /**
     * Removes specified document from these changes.
     * Used to remove changes from overlay when I receive data (that document) from server.
     * (More reactive result than remove whole overlay after confirmation. Testing such use. )
     * @param changes DocumentChanges to remove from this changes object
     */
    void removeChangesFromChanges(DocumentChanges changes){
        for (Document doc:changes.getAddedDocuments()) {
            removeDocumentFromChanges(doc);
        }
        for (Document doc:changes.getUpdatedDocuments()) {
            removeDocumentFromChanges(doc);
        }
        for (Document doc:changes.getRemovedDocuments()) {
            removeDocumentFromChanges(doc);
        }
    }

    /**
     * Removes specified document from these changes.
     * Used to remove changes from overlay when I receive data (that document) from server.
     * (More reactive result than remove whole overlay after confirmation. Testing such use. )
     * @param changesList list of DocumentChanges to remove from this changes object
     */
    void removeListOfChangesFromChanges(List<DocumentChanges> changesList){
        for (DocumentChanges changes:changesList) {
            removeChangesFromChanges(changes);
        }
    }

}


/*
*
*
* */