package net.jards.core;

import net.jards.errors.Error;

public interface RemoteStorageListener {

	void requestCompleted(ExecutionRequest request, Object result);

	void changesReceived(RemoteDocumentChange[] changes);
	
	void collectionInvalidated(String collection);

	void connectionChanged(Connection connection);

	void unsubscribed(String subscriptionName, Error error);

	void onError(Error error);
}

/*
*
*
* */