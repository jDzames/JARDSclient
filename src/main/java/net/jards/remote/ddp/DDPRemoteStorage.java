package net.jards.remote.ddp;

import com.keysolutions.ddpclient.DDPClient;
import com.keysolutions.ddpclient.EmailAuth;
import com.keysolutions.ddpclient.TokenAuth;
import com.keysolutions.ddpclient.UsernameAuth;
import net.jards.core.*;
import net.jards.errors.LocalStorageException;
import net.jards.errors.RemoteStorageError;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Implements RemoteStorage.
 * Class for work with remote server. Contains methods to connect, subscribe and call methods on server.
 * Contains DDPClient and DDPObserver from https://github.com/kenyee/java-ddp-client.
 */
public class DDPRemoteStorage extends RemoteStorage {

    /**
     * server address
     */
    private final String serverAddress;
    /**
     * port on which server listens
     */
    private final int serverPort;
    /**
     * DDP login type
     */
    private final DDPConnectionSettings.LoginType loginType;
    /**
     * specified token if its corresponding type
     */
    private final String resumeToken;
    /**
     * specified username if its corresponding type
     */
    private final String userName;
    /**
     * specified email if its corresponding type
     */
    private final String email;
    /**
     * specified password
     */
    private final String password;
    /**
     * last session from remote storage
     */
    private String session;

    /**
     * DDP client that sends requests to server
     */
    private DDPClient ddpClient;
    /**
     * DDP observer that listens for data from server
     */
    private DDPObserver ddpObserver;
    /**
     * RemoteStorageListener that is notified about changes and information froms server
     * and sends it to Storage
     */
    private RemoteStorageListener remoteStorageListener;

    /**
     * map of subscriptions (with subscription id as key - supports multiple subscriptions with same name)
     */
    private final Map<Integer, ExecutionRequest> subscriptions;
    /**
     * map with unconfirmed requests
     */
    private final Map<Integer, ExecutionRequest> methods;
    /**
     * map for apply changes method which sends many requests to server (one for each change),
     * counter for those changes to know when it is done
     */
    private final Map<Integer, Integer> executeMethodsCount;

    /**
     * true if system is connected
     */
    private boolean systemIsConnected = false;

    /**
     * true if system was connected, and also disconnected after that
     */
    private boolean systemWasConnected = false;
    //private boolean systemWasDisconnected = false;

    /**
     * true if subscribe was used, serves to invalidate all local collections when
     * we get data from new subscription
     */
    private boolean subscribed_askedForNewDataset = false;

    /**
     * Creates DDPRemoteStorage with given parameters.
     * @param storageSetup with settings
     * @param connectionSettings containing server adress, port and login information
     */
    public DDPRemoteStorage(StorageSetup storageSetup, DDPConnectionSettings connectionSettings){
		this.serverAddress = connectionSettings.getServerAddress();
        this.serverPort = connectionSettings.getServerPort();
        this.loginType = connectionSettings.getLoginType();
        this.resumeToken = connectionSettings.getResumeToken();
        this.userName = connectionSettings.getUserName();
        this.email = connectionSettings.getEmail();
        this.password = connectionSettings.getPassword();

		subscriptions = new HashMap<>();
		methods = new HashMap<>();
        executeMethodsCount = new HashMap<>();
        session = null;

        //setReadyForConnect();
	}

    /**
     * Creates DDPRemoteStorage with given parameters.
     * @param storageSetup with settings
     * @param connectionSettings containing server address, port and login information
     * @param session String containing information about saved session, used to continue work with server.
     */
    public DDPRemoteStorage(StorageSetup storageSetup, DDPConnectionSettings connectionSettings, String session){
        this.serverAddress = connectionSettings.getServerAddress();
        this.serverPort = connectionSettings.getServerPort();
        this.loginType = connectionSettings.getLoginType();
        this.resumeToken = connectionSettings.getResumeToken();
        this.userName = connectionSettings.getUserName();
        this.email = connectionSettings.getEmail();
        this.password = connectionSettings.getPassword();

        subscriptions = new HashMap<>();
        methods = new HashMap<>();
        executeMethodsCount = new HashMap<>();
        this.session = session;

        //setReadyForConnect();
    }

    /**
     * Helper method to create objects from used library.
     */
    private void setReadyForConnect(){
        try {
            ddpClient = new DDPClient(serverAddress, serverPort);

            ddpObserver = new DDPObserver(this, session);
            ddpClient.addObserver(ddpObserver);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to server, logs user (if login information provided) and starts to listen for changes from server.
     * @param sessionState session to reconnect on server, if null clients starts new connection
     */
    @Override
	protected void start(String sessionState) {
        systemIsConnected = false;
        while (!systemIsConnected) {
            try {
                if (ddpClient==null){
                    setReadyForConnect();
                    ddpClient.connect();
                    subscribed_askedForNewDataset = false;
                } else {
                    if (!ddpClient.getState().equals(DDPClient.CONNSTATE.Connected)){
                        //setReadyForConnect();
                        ddpClient.disconnect();
                        Thread.sleep(750);
                        ddpClient.connect();
                        //subscribed_askedForNewDataset = false;
                    }
                }
                Thread.sleep(750);
                if (ddpClient.getState().equals(DDPClient.CONNSTATE.Connected)){
                    systemIsConnected = true;
                }

                // session
                // https://forums.meteor.com/t/meteor-passing-session-values-from-client-to-server/5716
                //http://stackoverflow.com/questions/30852792/meteor-passing-session-values-from-client-to-server
                //... do it for library and sent it to them?

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

	}

    /**
     * Disconnects from server and stops
     */
    @Override
	public void stop() {
		//TODO unsubscribe all subscriptions and such?
        ddpClient.disconnect();
        ddpClient.deleteObservers();
	}

    /**
     * Subscribes to a collection on server. Called by Storage.
     * @param subscriptionName name of collection
     * @param executionRequest request with additional arguments
     * @return id of subscription
     */
    @Override
	protected int subscribe(String subscriptionName, ExecutionRequest executionRequest) {
        if (subscriptions.containsKey(executionRequest.getRemoteCallsId())){
            subscriptions.remove(executionRequest.getRemoteCallsId());
        }
        subscribed_askedForNewDataset = true;
        int subId = ddpClient.subscribe(subscriptionName, executionRequest.getAttributes()); //new Object[]{});
        executionRequest.setRemoteCallsId(subId);
		subscriptions.put(subId, executionRequest);


        return subId;
	}

    /**
     * Unsubscribes selected subscription. Called by Storage.
     * @param request execution request with id of this subscription
     */
    @Override
    protected void unsubscribe(ExecutionRequest request){
		if (!subscriptions.containsKey(request.getRemoteCallsId())){
			//TODO exception? already unsubscribed?
            return;
		}
		/*int id = */ddpClient.unsubscribe(request.getRemoteCallsId());
	}

    /**
     * Set listener, where will be actions from DDPObserver delegated.
     * @param listener provided listener
     */
    @Override
	protected void setListener(RemoteStorageListener listener) {
		this.remoteStorageListener = listener;
	}

    /**
     * Call method on server
     * @param method method name
     * @param arguments method parametres
     * @param idSeed seed for algorithm which generates id of documents on server (to get same id on server and client)
     * @param request Storage-made request for this method
     */
    @Override
	protected void call(String method, Object[] arguments, String idSeed, ExecutionRequest request) {
        Object[] argsWithSeed;
        if (arguments != null){
            argsWithSeed = new Object[arguments.length+1];
            System.arraycopy(arguments,0, argsWithSeed, 0, arguments.length);
        } else {
            argsWithSeed = new Object[1];
        }
        argsWithSeed[argsWithSeed.length-1] = idSeed;
        int methodId = ddpClient.call(method, argsWithSeed);
        request.setRemoteCallsId(methodId);
		methods.put(methodId, request);
		//ddpObserver.addMethod(methodId, method);
	}

    /**
     * Apply changes that happened in local Storage to the remote server
     * @param changes changes of one document
     * @param request request that caused this change
     */
    @Override
	protected void applyChanges(DocumentChanges changes, ExecutionRequest request) {
        //http://stackoverflow.com/questions/31631810/access-denied-403-when-updating-user-accounts-client-side-in-meteor

        //id to know when are all done (decrement count for that id in map for execute calls)
        int randomId = UUID.randomUUID().hashCode();
        request.setRemoteCallsId(randomId);
        int count = 0;

		// Add documents
        for (Document document :changes.getAddedDocuments()) {
            String collectionName = document.getCollection().getName();
            Map<String, Object> documentMap = new HashMap<>();
            documentMap.put("_id", document.getId());
            documentMap.put("collection", collectionName);
            documentMap.put("jsondata", document.getContent());
            int methodId = ddpClient.collectionInsert(collectionName, documentMap);
            methods.put(methodId, request);
            //ddpObserver.addMethod(methodId, "collectionInsert");
            count++;
        }
        // Update documents
        for (Document document :changes.getUpdatedDocuments()) {
            String collectionName = document.getCollection().getName();
            Map<String, Object> documentMap = new HashMap<>();
            documentMap.put("_id", document.getId());
            documentMap.put("collection", collectionName);
            documentMap.put("jsondata", document.getContent());
            String docId = document.getId();
            int methodId = ddpClient.collectionUpdate(collectionName, docId, documentMap);
            methods.put(methodId, request);
            //ddpObserver.addMethod(methodId, "collectionUpdate");
            count++;
        }
        // Remove documents
        for (Document document :changes.getRemovedDocuments()) {
            String collectionName = document.getCollection().getName();
            String docId = document.getId();
            int methodId = ddpClient.collectionDelete(collectionName, docId);
            methods.put(methodId, request);
            //ddpObserver.addMethod(methodId, "collectionDelete");
            count++;
        }

        //add count to map for this id
        executeMethodsCount.put(randomId, count);
	}

    /**
     * Get session state which was sent to this client from server
     * @return session state in String
     */
    @Override
	public String getSessionState() {
		return this.session;
	}

    /**
     * Provides DDP id generator with specified seed.
     * @param seed seed for generator
     * @return DDPIdGenerator object that can be used
     */
    @Override
    public IdGenerator getIdGenerator(String seed) {
        return new DDPIdGenerator(seed);
    }

    /**
     * Method called when this subscription becomes ready (all data first time arrived into the client)
     * @param subscriptionId id of ready subscription
     */
    void subscriptionReady(int subscriptionId){
        if (subscriptions.containsKey(subscriptionId)){
            remoteStorageListener.requestCompleted(subscriptions.get(subscriptionId));
        }
	}


    /**
     * Called when result message from server comes. It is sent into attached RemoteStorageListener.
     * @param methodId id of method
     */
    void requestCompleted(Integer methodId){
        //Integer methodIdInt = Integer.parseInt(methodId);
        if (methods.containsKey(methodId)){
            ExecutionRequest request = methods.get(methodId);
            if (request.isExecute()){
                //if it is execute - there are more calls - listener method after last one is done
                int actualCount = executeMethodsCount.get(request.getRemoteCallsId());
                //methodId is id on calls map, given my ddpClient. request's id is for execute calls
                actualCount--;
                if (actualCount <= 0){
                    this.remoteStorageListener.requestCompleted(methods.get(methodId));
                }
            } else {
                this.remoteStorageListener.requestCompleted(methods.get(methodId));
            }
            methods.remove(methodId);
        }
	}

    /**
     * Called when DDPObserver receives some data from server. It is sent into attached RemoteStorageListener.
     * @param changes array of changes - in this situation it is array of length 1 with one added/updated/removed document
     */
    void changesReceived(RemoteDocumentChange[] changes){
		if (subscribed_askedForNewDataset){
            //reset local data, cause new dataset is coming (we subscribed, first data came)
            subscribed_askedForNewDataset = false;
            try {
                remoteStorageListener.collectionInvalidated(null);
            } catch (LocalStorageException e) {
                e.printStackTrace();
            }
        }
        this.remoteStorageListener.changesReceived(changes);
	}

    /**
     * Called when server confirms that collection is invalidated. It is sent into attached RemoteStorageListener.
     * @param collection name of collection which was removed
     * @throws LocalStorageException can be thrown when collection is being removed from local storage
     */
    void collectionInvalidated(String collection) throws LocalStorageException {
		this.remoteStorageListener.collectionInvalidated(collection);
	}

    /**
     * Called when connection changes. It is sent into attached RemoteStorageListener.
     * @param connection object with information about connection
     */
    void connectionChanged(Connection connection){
        //If I connected, get session
        if (connection.getState() == Connection.STATE.Connected &&
                connection.getCode().equals(Connection.CONNECTED_AFTER_BEING_DISCONNECTED)){
            this.session = connection.getSession();
            //If I just connected and also I have login parameters, then login now.
            if (loginType != DDPConnectionSettings.LoginType.NoLogin){
                login();
            }
            //I subscribe to all subscriptions (and set their id, cause it can change), if I was offline
            if (systemWasConnected /*&& (session == null || session.length()==0)*/){
                /* reset local data, new dataset coming after subscribe
                try {
                    remoteStorageListener.collectionInvalidated(null);
                } catch (LocalStorageException e) {
                    e.printStackTrace();
                }*/
                systemWasConnected = false;
                //System.out.println("Subscribe sent on server after being offline");
                Map<Integer, ExecutionRequest> subs = new LinkedHashMap<>(subscriptions);
                subs.forEach((subId, request) ->
                        this.subscribe(request.getSubscriptionName(), request)
                );
            }
        }
        this.remoteStorageListener.connectionChanged(connection);
        //state other than connected and there are subscriptions that was used -> after reconnect subscribe again
        if (connection.getState()!=Connection.STATE.Connected && subscriptions.size()>0) {
            systemWasConnected = true;
        }
    }

    /**
     * Called when server stops subscription. It is sent into attached RemoteStorageListener.
     * @param subscriptionId id of stopped subscription
     * @param error error (optional, might be null)
     */
    void unsubscribed(int subscriptionId, RemoteStorageError error){
        if (subscriptions.containsKey(subscriptionId)){
            this.remoteStorageListener.unsubscribed(subscriptions.get(subscriptionId).getSubscriptionName(),
                    subscriptionId, error);
            this.remoteStorageListener.requestCompleted(subscriptions.get(subscriptionId));
            this.subscriptions.remove(subscriptionId);
        } else {
            this.remoteStorageListener.unsubscribed("", subscriptionId, error);
        }
	}

    /**
     * Called when server sends Error to client. It is sent into attached RemoteStorageListener.
     * @param error error from server
     */
    void onError(RemoteStorageError error){
		this.remoteStorageListener.onError(error);
	}


    /**
     * Help method for login. Sends login request on server. It is through token, username or email, the one of these which is provided.
     */
    private void login(){
        if (loginType == DDPConnectionSettings.LoginType.NoLogin){
            //return;
        } else if (loginType == DDPConnectionSettings.LoginType.Token){
            // token comes after login in first result message, save it as string
            TokenAuth tokenAuth = new TokenAuth(this.resumeToken);
            Object[] methodArgs = new Object[]{tokenAuth};
            ddpClient.call("login", methodArgs);
        } else if (loginType == DDPConnectionSettings.LoginType.Username){
            UsernameAuth usernameAuth = new UsernameAuth(this.userName, this.password);
            Object[] methodArgs = new Object[]{usernameAuth};
            ddpClient.call("login", methodArgs);
        } else if (loginType == DDPConnectionSettings.LoginType.Email){
            EmailAuth emailAuth = new EmailAuth(this.email, this.password);
            Object[] methodArgs = new Object[]{emailAuth};
            ddpClient.call("login", methodArgs);
        }
    }

}
