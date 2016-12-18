package net.jards.core;

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

	public Collection getCollection() {
		return collection;
	}

	public UUID getUuid() {
		return uuid;
	}
}
