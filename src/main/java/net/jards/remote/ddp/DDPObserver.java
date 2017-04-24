package net.jards.remote.ddp;

import com.google.gson.Gson;
import com.keysolutions.ddpclient.DDPClient;
import com.keysolutions.ddpclient.DDPClient.DdpMessageField;
import com.keysolutions.ddpclient.DDPClient.DdpMessageType;
import com.keysolutions.ddpclient.DDPListener;
import net.jards.core.Connection;
import net.jards.core.RemoteDocumentChange;

import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static net.jards.core.Connection.CONNECTED_AFTER_BEING_DISCONNECTED;
import static net.jards.core.Connection.STATE;

/**
 *
 * @author kenyee
 * edited by jdzama
 *
 * DDP client observer that handles enough messages for unit tests to work
 */
public class DDPObserver extends DDPListener implements Observer {

    public enum DDPSTATE {
        Disconnected,
        Connected,
        LoggedIn,
        Closed,
    }

    public DDPSTATE mDdpState;
    public String mResumeToken;
    public String mUserId;
    public int mErrorCode;
    public String mErrorType;
    public String mErrorReason;
    public String mErrorMsg;
    public String mErrorSource;
    public String mSessionId;
    public int mCloseCode;
    public String mCloseReason;
    public boolean mCloseFromRemote;
    public String mReadySubscription;
    public String mPingId;

    private STATE myDdpState;

    private String mSession;
    private String mToken;

    //public String mPingId;

    //private Map<Integer, String> methods;
    private final DDPRemoteStorage ddpRemoteStorage;
    private final Gson gson;


    public DDPObserver(DDPRemoteStorage ddpRemoteStorage, String session) {
        this.ddpRemoteStorage = ddpRemoteStorage;
        if (session!=null && !"".equals(session)){
            mSession = session;
            mSessionId= session;
        }
        myDdpState = STATE.Disconnected;
        gson = new Gson();
        mDdpState = DDPSTATE.Disconnected;
    }

    /**
     * Handles processing of DDP msgs
     */
    @SuppressWarnings("unchecked")
    public void update(Observable client, Object msg) {
        //System.out.println("----- MESSAGE full : "+msg);
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
                DefaultRemoteStorageError error = new DefaultRemoteStorageError(-1, mErrorSource, mErrorMsg);
                this.ddpRemoteStorage.onError(error);
            }
            if (msgtype.equals(DdpMessageType.CONNECTED)) {
                mDdpState = DDPSTATE.Connected;
                mSessionId = (String) jsonFields.get(DdpMessageField.SESSION);
                //--
                Integer code = null;
                if (myDdpState == STATE.Disconnected || myDdpState == STATE.Closed) {
                    System.out.println("             ------------    after disconect   --------------- ");
                    code = CONNECTED_AFTER_BEING_DISCONNECTED;
                }
                myDdpState = STATE.Connected;
                mSession = (String) jsonFields.get(DdpMessageField.SESSION);
                this.ddpRemoteStorage.connectionChanged(new Connection(STATE.Connected, mSession, code, null, null));
            }
            if (msgtype.equals(DdpMessageType.CLOSED)) {
                mDdpState = DDPSTATE.Closed;
                mCloseCode = Integer.parseInt(jsonFields.get(DdpMessageField.CODE).toString());
                mCloseReason = (String) jsonFields.get(DdpMessageField.REASON);
                mCloseFromRemote = (Boolean) jsonFields.get(DdpMessageField.REMOTE);
                //--
                myDdpState = STATE.Closed;

                this.ddpRemoteStorage.connectionChanged(new Connection(STATE.Closed, null, mCloseCode, mCloseReason, mCloseFromRemote));
            }
            if (msgtype.equals(DdpMessageType.READY)) {
                //msg -> array list -> index 0 ->string
                String mSubId = (String) ((ArrayList<Object>)jsonFields.get("subs")).get(0);
                //subscription.setReady(true);
                this.ddpRemoteStorage.subscriptionReady(Integer.parseInt(mSubId));
            }
            if (msgtype.equals(DdpMessageType.ADDED)) {
                String collectionName = (String) jsonFields.get(DdpMessageField.COLLECTION);
                String id = (String) jsonFields.get(DdpMessageField.ID);
                Map<String, Object> jsonMap = (Map<String, Object>) jsonFields.get(DdpMessageField.FIELDS);
                String jsonData;
                //if (jsonMap.containsKey("text")){
                    //jsonData = (String)jsonMap.get("text");
                //} else {
                jsonData = gson.toJson(jsonMap);
                //}
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
                /*if (jsonMap.containsKey("text")){
                    jsonData = (String)jsonMap.get("text");
                } else {
                    jsonData = gson.toJson(jsonMap);
                }*/
                RemoteDocumentChange documentChange = new RemoteDocumentChange(RemoteDocumentChange.ChangeType.UPDATE,
                        collectionName, id, jsonData);
                ddpRemoteStorage.changesReceived(new RemoteDocumentChange[]{documentChange});
            }
            //TODO: handle addedBefore, movedBefore??
            if (msgtype.equals(DdpMessageType.NOSUB)) {
                String id = (String) jsonFields.get(DdpMessageField.ID);
                Integer idInt = Integer.parseInt(id);
                Map<String, Object> error = (Map<String, Object>)jsonFields.get(DdpMessageField.ERROR);
                if (error != null) {
                    //int mErrorCode = (int) Math.round((Double)error.get("error"));
                    String mErrorMsg = (String) error.get("message");
                    //String mErrorType = (String) error.get("errorType");
                    String mErrorReason = (String) error.get("reason");
                    ddpRemoteStorage.unsubscribed(idInt, new DefaultRemoteStorageError(-1, "server", mErrorMsg+", "+mErrorReason));
                } else {
                    // if there's no error, it just means a subscription was unsubscribed
                    ddpRemoteStorage.unsubscribed(idInt, null);
                }
            }
            if (msgtype.equals(DdpMessageType.RESULT)) {
                //int methodId = Integer.parseInt((String) jsonFields.get(DdpMessageField.ID));
                if (jsonFields.containsKey(DdpMessageField.RESULT)){
                    Map<String, Object> resultFields = (Map<String, Object>) jsonFields.get(DdpMessageField.RESULT);
                    if (resultFields.containsKey("token")) {
                        // it was login method (not sure!)
                        mToken = (String) resultFields.get("token");
                        mUserId = (String) resultFields.get("id");
                        //
                        myDdpState = STATE.LoggedIn;
                        ddpRemoteStorage.connectionChanged(new Connection(STATE.LoggedIn, null, null, null, null));
                    }
                }
                if (jsonFields.containsKey(DdpMessageField.ERROR)) {
                    Map<String, Object> error = (Map<String, Object>) jsonFields.get(DdpMessageField.ERROR);
                    //Integer mErrorCode = (int) Math.round((Double)error.get("error"));
                    String mErrorMsg = (String) error.get("message");
                    //String mErrorType = (String) error.get("errorType");
                    String mErrorReason = (String) error.get("reason");
                    ddpRemoteStorage.onError(new DefaultRemoteStorageError(-1, "server", mErrorMsg + mErrorReason));
                }
                //methods.remove(methodId);
                //Library has probably problem when you send id from here somewhere else. No idea why
                //but next row produces error if id is not sent through other object (string here).
                //ddpRemoteStorage.requestCompleted(""+methodId, resultFields);
            }
            if (msgtype.equals(DdpMessageType.UPDATED)) {
                ArrayList<String> finishedMethods = (ArrayList<String>) jsonFields.get(DdpMessageField.METHODS);
                for (String id :finishedMethods) {
                    Integer intId = Integer.parseInt(id);
                    //methods.remove(intId);
                    ddpRemoteStorage.requestCompleted(intId);
                }
                //nothing to do here
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

    /*public STATE getmDdpState() {
        return mDdpState;
    }*/

    /*protected void addMethod(int methodId, String methodName) {
        this.methods.put(methodId, methodName);
    }

    protected void removeMethod(int methodId) {
        this.methods.remove(methodId);
    }*/

    @Override
    @SuppressWarnings("unchecked")
    public void onResult(Map<String, Object> jsonFields) {
        //NOTE: in normal usage, you'd add a listener per command, not a global one like this
        // handle method data collection updated msg
        String methodId = (String) jsonFields.get(DdpMessageField.ID);
        if (methodId.equals("1") && jsonFields.containsKey("result")) {
            Map<String, Object> result = (Map<String, Object>) jsonFields.get(DdpMessageField.RESULT);
            // login method is always "1"
            // REVIEW: is there a better way to figure out if it's a login result?
            mResumeToken = (String) result.get("token");
            mUserId = (String) result.get("id");
            mDdpState = DDPSTATE.LoggedIn;
        }
        if (jsonFields.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) jsonFields.get(DdpMessageField.ERROR);
            mErrorCode = (int) Math.round((Double)error.get("error"));
            mErrorMsg = (String) error.get("message");
            mErrorType = (String) error.get("errorType");
            mErrorReason = (String) error.get("reason");
        }
    }

    @Override
    public void onNoSub(String id, Map<String, Object> error) {
        if (error != null) {
            mErrorCode = (int) Math.round((Double)error.get("error"));
            mErrorMsg = (String) error.get("message");
            mErrorType = (String) error.get("errorType");
            mErrorReason = (String) error.get("reason");
        } else {
            // if there's no error, it just means a subscription was unsubscribed
            mReadySubscription = null;
        }
    }

    @Override
    public void onReady(String id) {
        mReadySubscription = id;
    }

    @Override
    public void onPong(String id) {
        mPingId = id;
    }
}
