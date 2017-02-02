package net.jards.core;

import net.jards.local.sqlite.SqliteException;

public abstract class LocalStorage {

	public LocalStorage(StorageSetup storageSetup) {
		
	}

    public abstract void removeCollection(String collection) throws SqliteException;


}

/*
*
*
* */