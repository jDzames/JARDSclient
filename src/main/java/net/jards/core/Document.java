package net.jards.core;

import net.jards.errors.JsonFormatException;

import java.util.*;

public class Document {

	private static final JSONPropertyExtractor DEFAULT_PROPERTY_EXTRACTOR = new DefaultJSONPropertyExtractor();

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

	private final JSONPropertyExtractor propertyExtractor = DEFAULT_PROPERTY_EXTRACTOR;

	/**
	 * Map with pre-fetched property values.
	 */
	private Map<String, Object> propertyCache = null;

	/**
	 * Constructs a document that is not associated with any collection.
	 */
	public Document(String content) {
		this.collection = null;
		id = null;
        if (content==null){
            jsonData = "";
        } else {
            jsonData = content;
        }
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
		if (propertyCache != null) {
			propertyCache.clear();
		}
	}

	public Object getPropertyValue(String propertyName) throws JsonFormatException {
		Object result = null;

		if ((propertyCache != null) && propertyCache.containsKey(propertyName)) {
			result = propertyCache.get(propertyName);
		} else {
			result = propertyExtractor.extractPropertyValue(jsonData, propertyName);
		}

		if (propertyCache != null) {
			propertyCache.put(propertyName, result);
		}

		return result;
	}

	public void prefetchProperties(List<String> propertyNames) {
		Set<String> propertyNameSet = new HashSet<>(propertyNames);
		if (propertyCache != null) {
			propertyNameSet.removeAll(propertyCache.keySet());
		}

		if (propertyNameSet.isEmpty()) {
			return;
		}

        Map<String, Object> extractedProperties = null;
        try {
            extractedProperties = propertyExtractor.extractPropertyValues(jsonData,
                    new ArrayList<String>(propertyNameSet));
        } catch (JsonFormatException e) {
            e.printStackTrace();
        }

        if (propertyCache == null) {
			propertyCache = new HashMap<>();
		}

		propertyCache.putAll(extractedProperties);
	}

	public Map<String, String> toMap() {
		Map<String, String> docMap = new HashMap<>();
		docMap.put("id", this.id);
		docMap.put("collection", this.collection.getName());
		docMap.put("jsonData", this.jsonData);
		return docMap;
	}

    @Override
    public String toString() {
        return "{"+super.toString() + ": { ID:"+id+", COLLECTION:"+collection.getName()+", JSONDATA:"+jsonData+" }";
    }
}

/*
 * doplnit
 *
 */