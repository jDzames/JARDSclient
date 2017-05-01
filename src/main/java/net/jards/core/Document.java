package net.jards.core;

import net.jards.errors.JsonFormatException;

import java.util.*;

/**
 * Class representing document in our system - base unit holding data.
 */
public class Document {

    /**
     * default property extractor
     */
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
	 * Raw (JSON) data, content of document.
	 */
	private String content;

    /**
     * extractor for this document
     */
    private final JSONPropertyExtractor propertyExtractor = DEFAULT_PROPERTY_EXTRACTOR;

	/**
	 * Map with pre-fetched property values.
	 */
	private Map<String, Object> propertyCache = null;

	/**
	 * Constructs a document that is not associated with any collection, neither has id.
	 */
    Document() {
        this.collection = null;
        id = null;
        content = "";
    }

    /**
     * Creates document without collection or id, only with content.
     * @param content content for this document
     */
    public Document(String content) {
		this.collection = null;
		id = null;
        if (content==null){
            this.content = "";
        } else {
            this.content = content;
        }
	}

	/**
	 * Package protected constructor of a document in a collection.
	 * 
	 * @param collection the collection where the document has been inserted
	 * @param id the identifier of the document.
	 */
	Document(Collection collection, String id) {
		this.collection = collection;
		this.id = id;
	}

    /**
     * Creates document from map; used in Collection.
     * @param documentMap map containing document content, id and collection
     * @param storage storage reference
     */
    Document(Map<String, String> documentMap, Storage storage) {
		this.collection = storage.getCollection(documentMap.get("collection"));
		this.id = documentMap.get("id");
		this.content = documentMap.get("jsondata");
	}

    /**
     * @return this document's collection reference
     */
    public Collection getCollection() {
		return collection;
	}

    /**
     * @return id of this document
     */
    public String getId() {
		return id;
	}

    /**
     * @return content of this document in string format
     */
    public String getContent() {
		return content;
	}

    /**
     * Sets document content to specified content
     * @param content content for this document
     */
    void setContent(String content) {
		this.content = content;
		if (propertyCache != null) {
			propertyCache.clear();
		}
	}

    /**
     * Gets value of specified property from this document content.
     * @param propertyName name of property
     * @return value of specified property
     * @throws JsonFormatException exception thrown if problem with parsing content happens in extractor
     */
    public Object getPropertyValue(String propertyName) throws JsonFormatException {
		Object result = null;

		if ((propertyCache != null) && propertyCache.containsKey(propertyName)) {
			result = propertyCache.get(propertyName);
		} else {
			result = propertyExtractor.extractPropertyValue(content, propertyName);
		}

		if (propertyCache != null) {
			propertyCache.put(propertyName, result);
		}

		return result;
	}

    /**
     * Prefetches properties values in this document.
     * @param propertyNames list of property names
     */
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
            extractedProperties = propertyExtractor.extractPropertyValues(content,
                    new ArrayList<>(propertyNameSet));
        } catch (JsonFormatException e) {
            e.printStackTrace();
        }

        if (propertyCache == null) {
			propertyCache = new HashMap<>();
		}

		propertyCache.putAll(extractedProperties);
	}

    /**
     * Creates map from this document.
     * @return map created from this document
     */
    public Map<String, String> toMap() {
		Map<String, String> docMap = new HashMap<>();
		docMap.put("id", this.id);
		docMap.put("collection", this.collection.getName());
		docMap.put("content", this.content);
		return docMap;
	}

    /**
     * @return String representation of this document.
     */
    @Override
    public String toString() {
        return "{"+super.toString() + ": { ID:"+id+", COLLECTION:"+collection.getName()+", JSONDATA:"+ content +" }";
    }
}
