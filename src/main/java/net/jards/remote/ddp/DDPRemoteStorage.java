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


    public DDPRemoteStorage(StorageSetup storageSetup, DDPConnectionSettings connectionSettings){
		this.serverAdress = connectionSettings.getServerAdress();
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

    public DDPRemoteStorage(StorageSetup storageSetup, DDPConnectionSettings connectionSettings, String session){
        this.serverAdress = connectionSettings.getServerAdress();
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

	@Override
	protected void start(String sessionState) {

		try {
			ddpClient = new DDPClient(serverAdress, serverPort);

			ddpObserver = new DDPObserver(this);
			ddpClient.addObserver(ddpObserver);
			ddpClient.connect();

            //TODO session - ako to poslat serveru?
            // https://forums.meteor.com/t/meteor-passing-session-values-from-client-to-server/5716


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
		int subId = ddpClient.subscribe(subscriptionName, arguments); //new Object[]{});
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


	protected void requestCompleted(String methodId, Object result){
        Integer methodIdInt = Integer.parseInt(methodId);
        if (methods.containsKey(methodIdInt)){
            this.remoteStorageListener.requestCompleted(methods.get(methodIdInt), result);
            methods.remove(methodIdInt);
        }
	}

	protected void changesReceived(RemoteDocumentChange[] changes){
		this.remoteStorageListener.changesReceived(changes);
	}

	protected void collectionInvalidated(String collection){
		this.remoteStorageListener.collectionInvalidated(collection);
	}

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

	protected void unsubscibed(String subscriptionName, Error error){
		this.subscriptions.remove(subscriptionName);
		this.remoteStorageListener.unsubscribed(subscriptionName, error);
	}

	protected void onError(Error error){
		this.remoteStorageListener.onError(error);
	}


    /**
     * Help method for login.
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