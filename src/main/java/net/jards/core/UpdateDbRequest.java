package net.jards.core;

/**
 * Class for remote requests for changes.
 */
public class UpdateDbRequest {

    /**
     * changes from server
     */
    private DocumentChanges documentChanges;
    /**
     * collection that should be invalidated if it's that type
     */
    private String collectionName;

    /**
     * true if this request is invalidate collection type
     */
    private boolean invalidateCollection = false;

    /**
     * Empty constructor.
     */
    public UpdateDbRequest() {
    }

    /**
     * @return name of specified collection
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @param collectionName specify name of collection to invalidate
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * @param invalidateCollection if true, sets type of this request to invalidate collection
     */
    public void setInvalidateCollection(boolean invalidateCollection) {
        this.invalidateCollection = invalidateCollection;
    }

    /**
     * @return true if it's of type invalidate collection false else
     */
    public boolean isInvalidateCollection() {
        return invalidateCollection;
    }

    /**
     * @return document changes from server in this request
     */
    public DocumentChanges getDocumentChanges() {
        return documentChanges;
    }

    /**
     * @param documentChanges document changes from server
     */
    public void setDocumentChanges(DocumentChanges documentChanges) {
        this.documentChanges = documentChanges;
    }
}
