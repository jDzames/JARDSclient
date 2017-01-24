package net.jards.remote.ddp;

import com.google.gson.Gson;
import com.keysolutions.ddpclient.DDPClient;
import com.keysolutions.ddpclient.DDPClient.DdpMessageField;
import com.keysolutions.ddpclient.DDPClient.DdpMessageType;
import com.keysolutions.ddpclient.DDPListener;
import net.jards.core.Connection;
import net.jards.core.RemoteDocumentChange;
import net.jards.errors.DefaultError;
import net.jards.errors.Error;

import java.util.*;

import static net.jards.core.Connection.STATE;

/**
 * @author kenyee
 *
 * DDP client observer that handles enough messages for unit tests to work
 */
public class DDPObserver extends DDPListener implements Observer {


    public STATE mDdpState;
    public String mSession;
    private String mToken;
    private String mUserId;
    public String mPingId;

    private Map<Integer, DDPSubscription> subscriptions;
    private Map<Integer, String> methods;
    private final DDPRemoteStorage ddpRemoteStorage;
    private final Gson gson;


    public DDPObserver(DDPRemoteStorage ddpRemoteStorage) {
        this.ddpRemoteStorage = ddpRemoteStorage;
        subscriptions = new HashMap<>();
        methods = new HashMap<>();
        mDdpState = STATE.Disconnected;
        gson = new Gson();
    }

    /**
     * Handles processing of DDP msgs
     */
    @SuppressWarnings("unchecked")
    public void update(Observable client, Object msg) {
        System.out.println("----- MESSAGE full : "+msg);
        if (msg instanceof Map<?, ?>) {
            Map<String, Object> jsonFields = (Map<String, Object>) msg;
            // handle msg types for DDP server->client msgs: https://github.com/meteor/meteor/blob/master/packages/livedata/DDP.md
            String msgtype = (String) jsonFields.get(DDPClient.DdpMessageField.MSG);
            if (msgtype == null) {
                // ignore {"server_id":"GqrKrbcSeDfTYDkzQ"} web socket msgs
                return;
            }
            if (msgtype.equals(DdpMessageType.ERROR)) {
                String mErrorSource = (String) jsonFields.get(DdpMessageField.SOURCE);
                String mErrorMsg = (String) jsonFields.get(DdpMessageField.ERRORMSG);
                Error error = new DefaultError(-1, mErrorSource, mErrorMsg);
                this.ddpRemoteStorage.onError(error);
            }
            if (msgtype.equals(DdpMessageType.CONNECTED)) {
                mDdpState = STATE.Connected;
                mSession = (String) jsonFields.get(DdpMessageField.SESSION);
                this.ddpRemoteStorage.connectionChanged(new Connection(STATE.Connected, mSession, null, null, null));
            }
            if (msgtype.equals(DdpMessageType.CLOSED)) {
                mDdpState = STATE.Closed;
                Integer mCloseCode = Integer.parseInt(jsonFields.get(DdpMessageField.CODE).toString());
                String mCloseReason = (String) jsonFields.get(DdpMessageField.REASON);
                Boolean mCloseFromRemote = (Boolean) jsonFields.get(DdpMessageField.REMOTE);
                this.ddpRemoteStorage.connectionChanged(new Connection(STATE.Closed, null, mCloseCode, mCloseReason, mCloseFromRemote));
            }
            if (msgtype.equals(DdpMessageType.READY)) {
                //msg -> array list -> index 0 ->string
                String mSubId = (String) ((ArrayList<Object>)jsonFields.get("subs")).get(0);
                if (!subscriptions.containsKey(mSubId)){
                    //TODO error, nothing?
                    return;
                }
                DDPSubscription subscription = subscriptions.get(mSubId);
                subscription.setReady(true);
                this.ddpRemoteStorage.subscriptionReady(subscription.getSubscriptionName());
            }
            if (msgtype.equals(DdpMessageType.ADDED)) {
                String collectionName = (String) jsonFields.get(DdpMessageField.COLLECTION);
                String id = (String) jsonFields.get(DdpMessageField.ID);
                Map<String, Object> jsonMap = (Map<String, Object>) jsonFields.get(DdpMessageField.FIELDS);
                String jsonData = gson.toJson(jsonMap);
                RemoteDocumentChange documentChange = new RemoteDocumentChange(RemoteDocumentChange.ChangeType.INSERT,
                        collectionName, id, jsonData);
                ddpRemoteStorage.changesReceived(new RemoteDocumentChange[]{documentChange});
            }
            if (msgtype.equals(DdpMessageType.REMOVED)) {
                String collectionName = (String) jsonFields.get(DdpMessageField.COLLECTION);
                String id = (String) jsonFields.get(DdpMessageField.ID);
                RemoteDocumentChange documentChange = new RemoteDocumentChange(RemoteDocumentChange.ChangeType.REMOVE,
                        collectionName, id, "");
                ddpRemoteStorage.changesReceived(new RemoteDocumentChange[]{documentChange});
            }
            if (msgtype.equals(DdpMessageType.CHANGED)) {
                String collectionName = (String) jsonFields.get(DdpMessageField.COLLECTION);
                String id = (String) jsonFields.get(DdpMessageField.ID);
                Map<String, Object> jsonMap = (Map<String, Object>) jsonFields.get(DdpMessageField.FIELDS);
                String jsonData = gson.toJson(jsonMap);
                RemoteDocumentChange documentChange = new RemoteDocumentChange(RemoteDocumentChange.ChangeType.UPDATE,
                        collectionName, id, jsonData);
                ddpRemoteStorage.changesReceived(new RemoteDocumentChange[]{documentChange});
            }
            //TODO: handle addedBefore, movedBefore??
            if (msgtype.equals(DdpMessageType.NOSUB)) {
                String id = (String) jsonFields.get(DdpMessageField.ID);
                if (!subscriptions.containsKey(id)){
                    //error?
                    return;
                }
                String subscriptionName = subscriptions.get(id).getSubscriptionName();
                subscriptions.remove(id);
                Map<String, Object> error = (Map<String, Object>)jsonFields.get(DdpMessageField.ERROR);
                if (error != null) {
                    int mErrorCode = (int) Math.round((Double)error.get("error"));
                    String mErrorMsg = (String) error.get("message");
                    String mErrorType = (String) error.get("errorType");
                    String mErrorReason = (String) error.get("reason");
                    ddpRemoteStorage.unsubscibed(subscriptionName, new DefaultError(-1, "server", mErrorMsg+", "+mErrorReason));
                } else {
                    // if there's no error, it just means a subscription was unsubscribed
                    ddpRemoteStorage.unsubscibed(subscriptionName, null);
                }
            }
            if (msgtype.equals(DdpMessageType.RESULT)) {
                int methodId = Integer.parseInt((String) jsonFields.get(DdpMessageField.ID));
                Map<String, Object> resultFields = (Map<String, Object>) jsonFields.get(DdpMessageField.RESULT);
                if (resultFields.containsKey("token")) {
                    // it was login method (nie iste!)
                    mToken = (String) resultFields.get("token");
                    mUserId = (String) resultFields.get("id");
                    //
                    mDdpState = STATE.LoggedIn;
                    ddpRemoteStorage.connectionChanged(new Connection(STATE.LoggedIn, null, null, null, null));
                }
                if (jsonFields.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) jsonFields.get(DdpMessageField.ERROR);
                    Integer mErrorCode = (int) Math.round((Double)error.get("error"));
                    String mErrorMsg = (String) error.get("message");
                    String mErrorType = (String) error.get("errorType");
                    String mErrorReason = (String) error.get("reason");
                    ddpRemoteStorage.onError(new DefaultError(-1, "server", mErrorMsg));
                }
                methods.remove(methodId);
                //Library has probably problem when you send id from here somewhere else. No idea why
                //but next row produces error if id is not sended through other object (string here).
                ddpRemoteStorage.requestCompleted(""+methodId, resultFields);
            }
            if (msgtype.equals(DdpMessageType.UPDATED)) {
                ArrayList<String> methods = (ArrayList<String>) jsonFields.get(DdpMessageField.METHODS);
                //TODO something to do here?
            }
            if (msgtype.equals(DdpMessageType.PING)) {
                //should work already
            }
            if (msgtype.equals(DdpMessageType.PONG)) {
                //should work already
            }
            if (msgtype.equals(DdpMessageType.CONNECT)) {
                System.err.println("server talks to me as to server");
            }
            if (msgtype.equals(DdpMessageType.METHOD)) {
                System.err.println("server talks to me as to server");
            }
            if (msgtype.equals(DdpMessageType.SUB)) {
                System.err.println("server talks to me as to server");
            }
            if (msgtype.equals(DdpMessageType.UNSUB)) {
                System.err.println("server talks to me as to server");
            }
        }

    }

    public STATE getmDdpState() {
        return mDdpState;
    }

    protected void addSubscription(int subId, DDPSubscription subscription) {
        this.subscriptions.put(subId, subscription);
    }

    protected void removeSubscription(int subId) {
        this.subscriptions.remove(subId);
    }

    protected void addMethod(int methodId, String methodName) {
        this.methods.put(methodId, methodName);
    }

    protected void removeMethod(int methodId) {
        this.methods.remove(methodId);
    }
}
