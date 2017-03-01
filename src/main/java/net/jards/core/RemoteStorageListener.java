package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.RemoteStorageError;

/**
 * Interface. Serves to listen to messages from server and send them to storage to proceed.
 */
public interface RemoteStorageListener {

	void requestCompleted(ExecutionRequest request);

	void changesReceived(RemoteDocumentChange[] changes);
	
	void collectionInvalidated(String collection) throws LocalStorageException;

	void connectionChanged(Connection connection);

	void unsubscribed(String subscriptionName, int subscriptionId, RemoteStorageError error);

	void onError(RemoteStorageError error);
}

/*
*
*
* */