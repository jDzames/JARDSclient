package net.jards.core;

import java.util.HashMap;
import java.util.Map;

public class Document {

	/**
	 * The collection to which the document belongs.
	 */
	private final Collection collection;

	/**
	 * Identifier of document.
	 */
	private final String id;



	/**
	 * Raw JSON data.
	 */
	private String jsonData;

	/**
	 * Constructs a document that is not associated with any collection.
	 */
	public Document() {
		this.collection = null;
		id = null;
	}

	/**
	 * Package protected constructor of a document in a collection.
	 * 
	 * @param collection
	 *            the collection where the document has been inserted
	 * @param id
	 *            the identifier of the document.
	 */
	Document(Collection collection, String id) {
		this.collection = collection;
		this.id = id;
	}

    Document(Map<String, String> documentMap, Storage storage) {
        this.collection = storage.getCollection(documentMap.get("collection"));
        this.id = documentMap.get("id");
        this.jsonData = documentMap.get("jsondata");
    }

	public Collection getCollection() {
		return collection;
	}

	public String getId() {
		return id;
	}

	public String getJsonData() {
		return jsonData;
	}

	void setJsonData(String jsonData) {
		this.jsonData = jsonData;
	}

    public Map<String,String> toMap() {
        Map<String, String> docMap = new HashMap<>();
        docMap.put("id", this.id);
        docMap.put("collection", this.collection.getName());
        docMap.put("jsonData", this.jsonData);
        return  docMap;
    }

}


/*
* doplnit
*
* */