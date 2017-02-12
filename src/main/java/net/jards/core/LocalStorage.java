package net.jards.core;

import net.jards.local.sqlite.SqliteException;

import java.util.Queue;

public abstract class LocalStorage {

	public LocalStorage(StorageSetup storageSetup) {
		
	}

    public abstract void connectDB() throws SqliteException;

    public abstract void addCollection(String collection) throws SqliteException;

    public abstract void removeCollection(String collection) throws SqliteException;

    public abstract String insert(String collectionName, Document document) throws SqliteException;

    public abstract String update(String collectionName, Document document) throws SqliteException;

    public abstract boolean remove(String collectionName, Document document) throws SqliteException;

    void start() {
        //TODO check if collections from storageSetup exists, if no - create them

    }

    void stop(Queue<ExecutionRequest> unconfirmedRequests) {
        //TODO save state - all from queue from thread (unconfirmed requests)

    }
}

/*
*
*
* */