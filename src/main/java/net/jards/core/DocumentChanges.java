package net.jards.core;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class DocumentChanges {

    private LinkedHashMap<String, Document> addedDocuments;
    private LinkedHashMap<String, Document> updatedDocuments;
    private LinkedHashMap<String, Document> removedDocuments;

    public DocumentChanges(){
        addedDocuments = new LinkedHashMap<>();
        updatedDocuments = new LinkedHashMap<>();
        removedDocuments = new LinkedHashMap<>();
    }

    public List<Document> getAddedDocuments() {
        List<Document> list = new LinkedList<>();
        list.addAll(addedDocuments.values());
        return list;
    }

    public List<Document> getUpdatedDocuments() {
        List<Document> list = new LinkedList<>();
        list.addAll(updatedDocuments.values());
        return list;
	}
	
	public List<Document> getRemovedDocuments() {
        List<Document> list = new LinkedList<>();
        list.addAll(removedDocuments.values());
        return list;
	}

	void addDocuments(List<Document> addedDocuments){
        for (Document doc:addedDocuments) {
            this.addedDocuments.put(doc.getId(), doc);
        }
    }

    void updateDocuments(List<Document> updatedDocuments){
        for (Document doc:updatedDocuments) {
            this.updatedDocuments.put(doc.getId(), doc);
        }
    }

    void addRemovedDocuments(List<Document> removedDocuments){
        for (Document doc:removedDocuments) {
            this.removedDocuments.put(doc.getId(), doc);
        }
    }

    void addDocument(Document document){
        this.addedDocuments.put(document.getId(), document);
    }

    void updateDocument(Document document){
        this.updatedDocuments.put(document.getId(), document);
    }

    void addRemovedDocument(Document document){
        this.removedDocuments.put(document.getId(), document);
    }

    void removeDocumentFromChanges(Document document){
        this.addedDocuments.remove(document.getId());
        this.updatedDocuments.remove(document.getId());
        this.removedDocuments.remove(document.getId());
    }

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