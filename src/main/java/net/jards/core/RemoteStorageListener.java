package net.jards.core;

import net.jards.errors.RemoteStorageError;

public interface RemoteStorageListener {

	void requestCompleted(ExecutionRequest request);

	void changesReceived(RemoteDocumentChange[] changes);
	
	void collectionInvalidated(String collection);

	void connectionChanged(Connection connection);

	void unsubscribed(String subscriptionName, RemoteStorageError error);

	void onError(RemoteStorageError error);
}

/*
*
*
* */