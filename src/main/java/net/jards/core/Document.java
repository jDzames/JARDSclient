package net.jards.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Document {

	/**
	 * The collection to which the document belongs.
	 */
	private final Collection collection;

	/**
	 * Identifier of document.
	 */
	private final UUID uuid;



	/**
	 * Raw JSON data.
	 */
	private String jsonData;

	/**
	 * Constructs a document that is not associated with any collection.
	 */
	public Document() {
		this.collection = null;
		uuid = null;
	}

	/**
	 * Package protected constructor of a document in a collection.
	 * 
	 * @param collection
	 *            the collection where the document has been inserted
	 * @param uuid
	 *            the identifier of the document.
	 */
	Document(Collection collection, UUID uuid) {
		this.collection = collection;
		this.uuid = uuid;
	}

    Document(Map<String, String> documentMap, Storage storage) {
        this.collection = storage.getCollection(documentMap.get("collection"));
        this.uuid = UUID.fromString(documentMap.get("id"));
        this.jsonData = documentMap.get("jsondata");
    }

	public Collection getCollection() {
		return collection;
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getJsonData() {
		return jsonData;
	}

	void setJsonData(String jsonData) {
		this.jsonData = jsonData;
	}

    public Map<String,String> toMap() {
        Map<String, String> docMap = new HashMap<>();
        docMap.put("id", this.uuid.toString());
        docMap.put("collection", this.collection.getName());
        docMap.put("jsonData", this.jsonData);
        return  docMap;
    }

}


/*
* doplnit
*
* */