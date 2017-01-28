package net.jards.core;

import java.util.LinkedList;
import java.util.List;

public class DocumentChanges {

    private List<Document> addedDocuments;
    private List<Document> updatedDocuments;
    private List<Document> removedDocuments;

    public DocumentChanges(){
        addedDocuments = new LinkedList<>();
        updatedDocuments = new LinkedList<>();
        removedDocuments = new LinkedList<>();
    }

    public List<Document> getAddedDocuments() {
        return addedDocuments;
    }

    public List<Document> getUpdatedDocuments() {
		return updatedDocuments;
	}
	
	public List<Document> getRemovedDocuments() {
		return removedDocuments;
	}

	void addDocuments(List<Document> addedDocuments){
        this.addedDocuments.addAll(addedDocuments);
    }

    void updateDocuments(List<Document> updatedDocuments){
        this.updatedDocuments.addAll(updatedDocuments);
    }

    void removeDocuments(List<Document> removedDocuments){
        this.removedDocuments.addAll(removedDocuments);
    }

    void addDocument(Document document){
        this.addedDocuments.add(document);
    }

    void updateDocument(Document document){
        this.updatedDocuments.add(document);
    }

    void removeDocument(Document document){
        this.removedDocuments.add(document);
    }

}


/*
*
*
* */