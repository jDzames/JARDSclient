package net.jards.remote.ddp;

import net.jards.core.Subscription;

/**
 * Class which provides handle to DDP subscriptions
 */
public class DDPSubscription implements Subscription {

    private final String subscriptionName;
    private final Object[] arguments;
    private int id;
    private boolean ready;
    private final DDPRemoteStorage remoteStorage;

    /**
     * Creates DDPSubscription
     * @param remoteStorage RemoteStorage which subscribed for this subscription
     * @param subscriptionName name of subscription
     * @param id id of subscription
     * @param arguments
     * @param ready ready if all subscription documents were already sent to client
     */
    public DDPSubscription(DDPRemoteStorage remoteStorage, String subscriptionName, int id, Object[] arguments, boolean ready) {
        //remote storage for unsubscribe
        this.remoteStorage = remoteStorage;
        this.subscriptionName = subscriptionName;
        this.arguments = arguments;
        this.id = id;
        this.ready = ready;
    }


    /**
     * Sets if this subscription is ready
     * @param ready
     */
    protected void setReady(boolean ready){
        this.ready = ready;
    }

    /**
     * Returns subscription name
     * @return name of this subscription
     */
    protected String getSubscriptionName(){
        return  this.subscriptionName;
    }

    /**
     * Stop to use this subscription. RemoteStorage wil unsubscribe.
     */
    @Override
    public void stop() {
        remoteStorage.unsubscribe(this.subscriptionName);
    }

    /**
     * Returns state of subscription
     * @return true/false - true if this subscription is ready, else false
     */
    @Override
    public boolean isReady() {
        return ready;
    }

    /**
     * Returns this subscription's id
     * @return id of this subscription
     */
    public int getId() {
        return id;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setId(int id) {
        this.id = id;
    }
}
