package net.jards.core;

public class RemoteDocumentChange {

	public enum ChangeType {
		INSERT, UPDATE, REMOVE
	}

	ChangeType type;

	String collection;
	
	String uuid;
	
	String data;

}
