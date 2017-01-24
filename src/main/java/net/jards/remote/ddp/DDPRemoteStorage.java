package net.jards.remote.ddp;

import com.keysolutions.ddpclient.DDPClient;
import com.keysolutions.ddpclient.EmailAuth;
import com.keysolutions.ddpclient.TokenAuth;
import com.keysolutions.ddpclient.UsernameAuth;
import net.jards.core.*;
import net.jards.errors.Error;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


/**
 * Implements RemoteStorage.
 * Class for work with remote server. Contains methods to connect, subscribe and call methods on server.
 * Contains DDPClient and DDPObserver from https://github.com/kenyee/java-ddp-client.
 */
public class DDPRemoteStorage extends RemoteStorage {

	private final String serverAdress;
	private final int serverPort;
    private final DDPConnectionSettings.LoginType loginType;
    private final String resumeToken;
    private final String userName;
    private final String email;
    private final String password;
    private String session;

    private DDPClient ddpClient;
    private DDPObserver ddpObserver;
    private RemoteStorageListener remoteStorageListener;

	private final Map<String, DDPSubscription> subscriptions;
	private final Map<Integer, ExecutionRequest> methods;


    /**
     * Creates DDPRemoteStorage with given parametres.
     * @param storageSetup with settings
     * @param connectionSettings containing server adress, port and login information
     */
    public DDPRemoteStorage(StorageSetup storageSetup, DDPConnectionSettings connectionSettings){
		this.serverAdress = connectionSettings.getServerAddress();
        this.serverPort = connectionSettings.getServerPort();
        this.loginType = connectionSettings.getLoginType();
        this.resumeToken = connectionSettings.getResumeToken();
        this.userName = connectionSettings.getUserName();
        this.email = connectionSettings.getEmail();
        this.password = connectionSettings.getPassword();

		subscriptions = new HashMap<>();
		methods = new HashMap<>();
        session = null;
	}

    /**
     * Creates DDPRemoteStorage with given parametres.
     * @param storageSetup with settings
     * @param connectionSettings containing server adress, port and login information
     * @param session String containing informations about saved session, used to continue work with server.
     */
    public DDPRemoteStorage(StorageSetup storageSetup, DDPConnectionSettings connectionSettings, String session){
        this.serverAdress = connectionSettings.getServerAddress();
        this.serverPort = connectionSettings.getServerPort();
        this.loginType = connectionSettings.getLoginType();
        this.resumeToken = connectionSettings.getResumeToken();
        this.userName = connectionSettings.getUserName();
        this.email = connectionSettings.getEmail();
        this.password = connectionSettings.getPassword();

        subscriptions = new HashMap<>();
        methods = new HashMap<>();
        this.session = session;
    }

    /**
     * Connects to server, logs user (if login informations provided) and starts to listen for changes from server.
     * @param sessionState session to reconnect on server, if null clients starts new connection
     */
    @Override
	protected void start(String sessionState) {
		try {
			ddpClient = new DDPClient(serverAdress, serverPort);

			ddpObserver = new DDPObserver(this);
			ddpClient.addObserver(ddpObserver);
			ddpClient.connect();

            //TODO session - ?
            // https://forums.meteor.com/t/meteor-passing-session-values-from-client-to-server/5716
            //http://stackoverflow.com/questions/30852792/meteor-passing-session-values-from-client-to-server
            //asi spravit pre kniznicu a poslat im

		} catch (URISyntaxException e) {
			e.printStackTrace();
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
     * @param arguments additional arguments
     * @return Subscription handle
     */
    @Override
	protected Subscription subscribe(String subscriptionName, Object... arguments) {
		int subId = ddpClient.subscribe(subscriptionName, arguments); //new Object[]{});
		DDPSubscription subscription = new DDPSubscription(this, subscriptionName, subId, false);
		subscriptions.put(subscriptionName, subscription);
		ddpObserver.addSubscription(subId, subscription);
		return subscription;
	}

    /**
     * Unsubscribes selected subscription. Called by Storage.
     * @param subscriptionName name of selected subscription
     */
    protected void unsubscribe(String subscriptionName){
		if (!subscriptions.containsKey(subscriptionName)){
			//TODO exception
		}
		int subId = subscriptions.get(subscriptionName).getId();
		ddpClient.unsubscribe(subId);
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
     * @param uuidSeed seed for algorithm which generates id of documents on server (to get same id on server and client)
     * @param request Storage-made request for this method
     */
    @Override
	protected void call(String method, Object[] arguments, String uuidSeed, ExecutionRequest request) {
		//TODO  seed? co je v storage ohladne toho?
        //check method name and arguments with request or no? or if request is not null?

		int methodId = ddpClient.call(method, arguments);
		methods.put(methodId, request);
		ddpObserver.addMethod(methodId, method);
	}

    /**
     * Apply changes that happened in local Storage to the remote server
     * @param changes changes of one document
     * @param request request that caused this change
     */
    @Override
	protected void applyChanges(DocumentChanges changes, ExecutionRequest request) {
		// TODO spravit

		//ddpClient.collectionInsert(); ...
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
     * Method called when this subscription becomes ready (all data first time arrived into the client)
     * @param subscriptionName name of selected subscription
     */
    protected void subscriptionReady(String subscriptionName){
		//TODO something to do?
        //ked v observeri pride
	}


    /**
     * Called when result message from server comes. It is sent into attached RemoteStorageListener.
     * @param methodId id of method
     * @param result result, null if server returned nothing
     */
    protected void requestCompleted(String methodId, Object result){
        Integer methodIdInt = Integer.parseInt(methodId);
        if (methods.containsKey(methodIdInt)){
            this.remoteStorageListener.requestCompleted(methods.get(methodIdInt), result);
            methods.remove(methodIdInt);
        }
	}

    /**
     * Called when DDPObserver receives some data from server. It is sent into attached RemoteStorageListener.
     * @param changes array of changes - in this situation it is array of length 1 with one added/updated/removed document
     */
    protected void changesReceived(RemoteDocumentChange[] changes){
		this.remoteStorageListener.changesReceived(changes);
	}

    /**
     * Called when server confirms that collection is invalidated. It is sent into attached RemoteStorageListener.
     * @param collection
     */
    protected void collectionInvalidated(String collection){
		this.remoteStorageListener.collectionInvalidated(collection);
	}

    /**
     * Called when connection changes. It is sent into attached RemoteStorageListener.
     * @param connection object with information about connection
     */
    protected void connectionChanged(Connection connection){
        //If I connected, get session
        if (connection.getState() == Connection.STATE.Connected ){
            this.session = connection.getSession();
            //If I just connected and also I have login parametres, then login now.
            if (loginType != DDPConnectionSettings.LoginType.NoLogin){
                login();
            }
        }
		this.remoteStorageListener.connectionChanged(connection);
	}

    /**
     * Called when server stops subscription. It is sent into attached RemoteStorageListener.
     * @param subscriptionName name of stopped subscription
     * @param error error (optional, might be null)
     */
    protected void unsubscibed(String subscriptionName, Error error){
		this.subscriptions.remove(subscriptionName);
		this.remoteStorageListener.unsubscribed(subscriptionName, error);
	}

    /**
     * Called when server sends Error to client. It is sent into attached RemoteStorageListener.
     * @param error error from server
     */
    protected void onError(Error error){
		this.remoteStorageListener.onError(error);
	}


    /**
     * Help method for login. Sends login request on server. It is through token, username or email, the one of these which is provided.
     */
    private void login(){
        if (loginType == DDPConnectionSettings.LoginType.NoLogin){
            return;
        } else if (loginType == DDPConnectionSettings.LoginType.Token){
            //TODO token pride po prvom prihlaseni v result sprave, je to string potom ked som ulozil
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

/*
* za tym uz je DDP Observer - v starte vytvorim
*
* */