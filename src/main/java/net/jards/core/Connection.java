package net.jards.core;

/**
 * Class holding actual connection information for Storage.
 */
public class Connection {

    /**
     * Enum for states of connection : Disconnected, Connected, LoggedIn, Closed.
     */
    public enum STATE {
        Disconnected,
        Connected,
        LoggedIn,
        Closed
    }

    /**
     * State of connection
     */
    private STATE state;
    /**
     * last session from server (if server supports it)
     */
    private String session;
    /**
     * code from remote storage
     */
    private Integer code;
    /**
     * reason of change (if server informs about it)
     */
    private String reason;
    /**
     * boolean informing about cause of last connection change (if it's remote cause)
     */
    private Boolean remote;

    /**
     * Used in remote storage to login and subscribe to server.
     */
    public static final Integer CONNECTED_AFTER_BEING_DISCONNECTED = 1;

    /**
     * Public constructor for connection
     * @param state actual state
     * @param sessionId last id from server
     * @param code code from RemoteStorage implementation
     * @param reason reason of connection change
     * @param remote boolean telling if change was initiated in remote
     */
    public Connection(STATE state, String sessionId, Integer code, String reason, Boolean remote){
        this.state = state;
        this.session = sessionId;
        this.code = code;
        this.reason = reason;
        this.remote = remote;
    }

    /**
     * @return state of this connection object
     */
    public STATE getState() {
        return state;
    }

    /**
     * @return code from remote server for this connection change
     */
    public Integer getCode() {
        return code;
    }

    /**
     * @return reason for last change of connection
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return last session from server
     */
    public String getSession() {
        return session;
    }

    /**
     * @return if this connection state was initiated in remote
     */
    public Boolean isRemote() {
        return remote;
    }
}
