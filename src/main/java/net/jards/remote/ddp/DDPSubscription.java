package net.jards.remote.ddp;

import net.jards.core.Subscription;

/**
 * Created by jDzama on 3.1.2017.
 */
public class DDPSubscription implements Subscription {

    private final String subscriptionName;
    private int id;
    private boolean ready;
    private final DDPRemoteStorage remoteStorage;

    public DDPSubscription(DDPRemoteStorage remoteStorage, String subscriptionName, int id,  boolean ready) {
        //remote storage for unsubscribe
        this.remoteStorage = remoteStorage;
        this.subscriptionName = subscriptionName;
        this.id = id;
        this.ready = ready;
    }


    protected void setReady(boolean ready){
        this.ready = ready;
    }

    protected String getSubscriptionName(){
        return  this.subscriptionName;
    }

    @Override
    public void stop() {
        remoteStorage.unsubscribe(this.subscriptionName);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    public int getId() {
        return id;
    }
}
