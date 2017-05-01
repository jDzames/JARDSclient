package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.RemoteStorageError;

/**
 * Interface. Implementation serves to listen to messages from server and send them to storage to proceed.
 */
public interface RemoteStorageListener {

    /**
     * Use in RemoteStorage implementation to inform about completion of some request execution on server
     * Save used requests in methods and send them through this method back to confirm end of execution.
     * @param request
     */
    void requestCompleted(ExecutionRequest request);

    /**
     * Use in RemoteStorage implementation to send changes from server (document representation)
     * @param changes array of changes (document representation)
     */
    void changesReceived(RemoteDocumentChange[] changes);

    /**
     * Use in RemoteStorage implementation to inform storage that collection have been invalidated
     * @param collection name of invalidated collection
     * @throws LocalStorageException error thrown in local storage while removing collection
     */
    void collectionInvalidated(String collection) throws LocalStorageException;

    /**
     * Use in RemoteStorage implementation to inform about connection change.
     * @param connection
     */
    void connectionChanged(Connection connection);

    /**
     * Use in RemoteStorage implementation to send Storage information that server ended subscription.
     * @param subscriptionName name of subscription
     * @param subscriptionId if of subscription (if client subscribed more times to one subscription
     *                         with different arguments). Not fully supported in whole system yet.
     * @param error optional error if was sent by server
     */
    void unsubscribed(String subscriptionName, int subscriptionId, RemoteStorageError error);

    /**
     * Use in RemoteStorage implementation to inform about server error.
     * @param error error from remote storage
     */
    void onError(RemoteStorageError error);
}

/*
*
*
* */