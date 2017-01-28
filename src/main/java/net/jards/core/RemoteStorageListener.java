package net.jards.core;

public interface RemoteStorageListener {

	void requestCompleted(ExecutionRequest request);

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