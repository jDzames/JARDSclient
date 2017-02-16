package net.jards.core;

/**
 * Abstract class. Serves to communicate with server, makes connection and sends requests on server.
 * If you use synchronous method which can took longer time, run it in new thread to not main program thread.
 */
public abstract class RemoteStorage {

	protected abstract void start(String sessionState);

	protected abstract void stop();

	protected abstract void setListener(RemoteStorageListener listener);

	protected abstract Subscription subscribe(String subscriptionName, Object[] arguments);

	protected abstract void unsubscribe(String subscriptionName);

	protected abstract void call(String method, Object[] arguments, String uuidSeed, ExecutionRequest request);

	protected abstract void applyChanges(DocumentChanges changes, ExecutionRequest request);

	public abstract String getSessionState();

    public abstract IdGenerator getIdGenerator(String seed);


}

/*
* seed - pozeral som, nedoriesil
*
* */