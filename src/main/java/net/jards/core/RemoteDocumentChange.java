package net.jards.core;

/**
 * Class representing one document change made on server, sent to Storage through RemoteStorageListener.
 */
public class RemoteDocumentChange {

    /**
     * Type of change: create, update, remove.
     */
    public enum ChangeType {
		INSERT, UPDATE, REMOVE
	}

    /**
     * Change type of this
     */
    private  ChangeType type;
    /**
     * collection of changed document
     */
    private  String collection;
    /**
     * id of changed document
     */
    private  String id;
    /**
     * content of changed document
     */
    private  String data;

    /**
     * Constructor for RemoteDocumentChange
     * @param type type of change
     * @param collection collection where corresponding document belongs
     * @param id if of corresponding document
     * @param data data of corresponding document
     */
    public RemoteDocumentChange(ChangeType type, String collection, String id, String data){
		this.type = type;
		this.collection = collection;
		this.id = id;
		this.data =data;
	}

    /**
     * @return type of change
     */
    public ChangeType getType() {
		return type;
	}

    /**
     * @return collection of corresponding document
     */
    public String getCollection() {
		return collection;
	}

    /**
     * @return content of corresponding document
     */
    public String getData() {
		return data;
	}

    /**
     * @return id of corresponding document
     */
    public String getId() {
		return id;
	}
}

/*
*
*
* */