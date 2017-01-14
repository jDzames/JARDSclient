package net.jards.remote.ddp;

import com.keysolutions.ddpclient.DDPClient;
import net.jards.core.*;
import net.jards.errors.Error;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class DDPRemoteStorage extends RemoteStorage {

	private String serverAdress;
	private int serverPort = 3000;
	private DDPClient ddpClient;
	private Map<String, DDPSubscription> subscriptions;
	private Map<Integer, ExecutionRequest> methods;
	private DDPObserver ddpObserver;
	private RemoteStorageListener remoteStorageListener;

	public DDPRemoteStorage(StorageSetup storageSetup, String serverAdress){
		this.serverAdress = serverAdress;
		subscriptions = new HashMap<>();
		methods = new HashMap<>();
	}

	public DDPRemoteStorage(StorageSetup storageSetup, String serverAdress, int serverPort){
		this.serverAdress = serverAdress;
		this.serverPort = serverPort;
		subscriptions = new HashMap<>();
		methods = new HashMap<>();
	}

	@Override
	protected void start(String sessionState) {
		// TODO Auto-generated method stub

		try {
			ddpClient = new DDPClient(serverAdress, serverPort);

			ddpObserver = new DDPObserver(this);
			ddpClient.addObserver(ddpObserver);
			ddpClient.connect();

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void stop() {
		//TODO unsubscribe all subscriptions and such?
		ddpClient.disconnect();
	}

	@Override
	protected Subscription subscribe(String subscriptionName, Object... arguments) {
		// TODO Auto-generated method stub
		int subId = ddpClient.subscribe(subscriptionName, new Object[]{});
		DDPSubscription subscription = new DDPSubscription(this, subscriptionName, subId, false);
		subscriptions.put(subscriptionName, subscription);
		ddpObserver.addSubscription(subId, subscription);
		return subscription;
	}

	protected void unsubscribe(String subscriptionName){
		if (!subscriptions.containsKey(subscriptionName)){
			//TODO exception
		}
		int subId = subscriptions.get(subscriptionName).getId();
		ddpClient.unsubscribe(subId);
	}

	@Override
	protected void setListener(RemoteStorageListener listener) {
		this.remoteStorageListener = listener;
	}

	@Override
	protected void call(String method, Object[] arguments, String uuidSeed, ExecutionRequest request) {
		//TODO  seed? co je v storage ohladne toho?

		int methodId = ddpClient.call(method, arguments);
		methods.put(methodId, request);
		ddpObserver.addMethod(methodId, method);
	}

	@Override
	protected void applyChanges(DocumentChanges changes, ExecutionRequest request) {
		// TODO

		//ddpClient.collectionInsert(); ...
	}

	@Override
	public String getSessionState() {
		//TODO to string? how take actual state and use it in restart?
		return ddpClient.getState().toString();
	}

	protected void subscriptionReady(String subscriptionName){
		//TODO something to do?
	}


	protected void requestCompleted(int methodId, Object result){
		this.remoteStorageListener.requestCompleted(methods.get(methodId), result);
		methods.remove(methodId);
	}

	protected void changesReceived(RemoteDocumentChange[] changes){
		this.remoteStorageListener.changesReceived(changes);
	}

	protected void collectionInvalidated(String collection){
		this.remoteStorageListener.collectionInvalidated(collection);
	}

	protected void connectionChanged(Connection connection){
		this.remoteStorageListener.connectionChanged(connection);
	}

	protected void unsubscibed(String subscriptionName, Error error){
		this.subscriptions.remove(subscriptionName);
		this.remoteStorageListener.unsubscribed(subscriptionName, error);
	}

	protected void onError(Error error){
		this.remoteStorageListener.onError(error);
	}

}

/*
* za tym uz je DDP Observer - v starte vytvorim
*
* */