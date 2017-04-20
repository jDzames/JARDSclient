package net.jards.core;

/**
 * Created by jDzama on 21.2.2017.
 */
public class UpdateDbRequest {

    private DocumentChanges documentChanges;
    private String collectionName;

    private boolean invalidateCollection = false;

    public UpdateDbRequest() {
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public void setInvalidateCollection(boolean invalidateCollection) {
        this.invalidateCollection = invalidateCollection;
    }

    public boolean isInvalidateCollection() {
        return invalidateCollection;
    }

    public DocumentChanges getDocumentChanges() {
        return documentChanges;
    }

    public void setDocumentChanges(DocumentChanges documentChanges) {
        this.documentChanges = documentChanges;
    }
}
