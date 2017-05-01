package net.jards.core;

/**
 * Abstract class. Serves to communicate with server, makes connection and sends requests on server.
 * If you use synchronous method which can took longer time, you can send those executions on new
 * threads (you can manage that yourself).
 *
 * Another possible extension in future: make 2 interface developers can use - one simple like this
 * (DefaultRemoteStorage or so) and second with thread and queues processing remote ExecutionRequests
 * so developers can manage or set up that themselves (RemoteStorage).
 */
public abstract class RemoteStorage {

    /**
     * Extend to start work of RemoteStorage implementation. Connection to server should be made here.
     * @param sessionState session specified by user, can be used
     */
    protected abstract void start(String sessionState);

    /**
     * Extend to stop work and disconnect froms server.
     */
    protected abstract void stop();

    /**
     * Set listener for your implementation. Remember to pass messages from server to this listener,
     * else system will not work.
     * @param listener listener used to pass data to system.
     */
    protected abstract void setListener(RemoteStorageListener listener);

    /**
     * Method for subscribe to server subscription (if server uses it).
     * @param subscriptionName name of subscription
     * @param request request, that should be sent to listener when subscription is confirmed
     * @return id for this subscription
     */
    protected abstract int subscribe(String subscriptionName, ExecutionRequest request);

    /**
     * Method to unsubscribe from selected
     * @param request subscription request that should be unsubscribed from
     */
    protected abstract void unsubscribe(ExecutionRequest request);

    /**
     * Method call on server
     * @param method method name
     * @param arguments arguments for this method call
     * @param idSeed seed for id generator
     * @param request request that should be then sent back with confirmation of execution
     */
    protected abstract void call(String method, Object[] arguments, String idSeed, ExecutionRequest request);

    /**
     * Method to apply changes on server (send added, updated and removed documents to server).
     * @param changes document changes
     * @param request request that should be sent back in listener on confirmation of execution
     */
    protected abstract void applyChanges(DocumentChanges changes, ExecutionRequest request);

    /**
     * @return last session state in string representation from server
     */
    public abstract String getSessionState();

    /**
     * Method to get id generator that will give same ids to local documents created in speculation.
     * <p>
     * Implement id generator matching id generating on server, so speculations can give
     * created documents same id (if possible). Else referencing on documents using id will not work
     * when referencing on document created in speculation.
     * @param seed seed for generator
     * @return id generator simulating server id generating
     */
    protected abstract IdGenerator getIdGenerator(String seed);


}
