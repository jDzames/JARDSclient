package net.jards.core;

import net.jards.local.sqlite.SqliteException;

public abstract class LocalStorage {

	public LocalStorage(StorageSetup storageSetup) {
		
	}

    public abstract void connectDB() throws SqliteException;

    public abstract void addCollection(String collection) throws SqliteException;

    public abstract void removeCollection(String collection) throws SqliteException;

    public abstract String insert(String collectionName, Document document) throws SqliteException;

    public abstract String update(String collectionName, Document document) throws SqliteException;

    public abstract boolean remove(String collectionName, Document document) throws SqliteException;

}

/*
*
*
* */