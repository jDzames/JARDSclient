package net.jards.core;

/**
 * Created by jDzama on 9.1.2017.
 */
public class Connection {

    public enum STATE {
        Disconnected,
        Connected,
        LoggedIn,
        Closed
    }

    private STATE state;
    private String session;
    private Integer code;
    private String reason;
    private Boolean remote;

    /**
     * Used in remote storage to login and subscribe to server.
     */
    public static final Integer CONNECTED_AFTER_BEING_DISCONNECTED = 1;

    public Connection(STATE state, String sessionId, Integer code, String reason, Boolean remote){
        this.state = state;
        this.session = sessionId;
        this.code = code;
        this.reason = reason;
        this.remote = remote;
    }

    public STATE getState() {
        return state;
    }

    public Integer getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public String getSession() {
        return session;
    }

    public Boolean isRemote() {
        return remote;
    }
}
