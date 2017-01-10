package net.jards.core;

public abstract class RemoteStorage {

	protected abstract Subscription subscribe(String subscriptionName, Object[] arguments);

	protected abstract void call(String method, Object[] arguments, String uuidSeed, ExecutionRequest request);

	protected abstract void applyChanges(DocumentChanges changes, ExecutionRequest request);

	protected abstract void start(String sessionState);

	protected abstract void stop(String subscriptionName);

	protected abstract void setListener(RemoteStorageListener listener);

	public abstract String getSessionState();
}

/*
* seed - pozeral som, nedoriesil
*
* */