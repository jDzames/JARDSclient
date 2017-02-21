package net.jards.core;

public class RemoteDocumentChange {

	public enum ChangeType {
		INSERT, UPDATE, REMOVE
	}

	private  ChangeType type;
	private  String collection;
	private  String id;
	private  String data;

	public RemoteDocumentChange(ChangeType type, String collection, String id, String data){
		this.type = type;
		this.collection = collection;
		this.id = id;
		this.data =data;
	}

	public ChangeType getType() {
		return type;
	}

	public String getCollection() {
		return collection;
	}

	public String getData() {
		return data;
	}

	public String getId() {
		return id;
	}
}

/*
*
*
* */