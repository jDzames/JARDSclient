package net.jards.core;

public interface RemoteStorageListener {

	void requestCompleted(ExecutionRequest request);

	void changesReceived(RemoteDocumentChange[] changes);
	
	void collectionInvalidated(String collection);

	void connectionChanged();
}
