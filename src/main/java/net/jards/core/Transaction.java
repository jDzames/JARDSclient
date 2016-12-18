package net.jards.core;

public class Transaction {

	private final Storage storage;

	Transaction(Storage storage) {
		this.storage = storage;
	}

	/*
	 * Package protected vs. protected?
	 */
	
	Document insert(Collection collection, Document document) {
		return null;
	}
	
	Document update(Collection collection, Document document) {
		return null;
	}
	
	Document remove(Collection collection, Document document) {
		return null;
	}
}
