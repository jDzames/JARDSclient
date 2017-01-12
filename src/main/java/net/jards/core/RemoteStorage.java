package net.jards.core;

public abstract class RemoteStorage {

	protected abstract void start(String sessionState);

	protected abstract void stop();

	protected abstract void setListener(RemoteStorageListener listener);

	protected abstract Subscription subscribe(String subscriptionName, Object[] arguments);

	protected abstract void unsubscribe(String subscriptionName);

	protected abstract void call(String method, Object[] arguments, String uuidSeed, ExecutionRequest request);

	protected abstract void applyChanges(DocumentChanges changes, ExecutionRequest request);

	public abstract String getSessionState();


}

/*
* seed - pozeral som, nedoriesil
*
* */