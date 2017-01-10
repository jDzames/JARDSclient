package net.jards.core;

/**
 * Created by jDzama on 9.1.2017.
 */
public class Connection {

    public enum STATE {
        Disconnected,
        Connected,
        LoggedIn,
        Closed,
    }

    private STATE state;
    private String sessionId;
    private Integer code;
    private String reason;
    private Boolean remote;

    public Connection(STATE state, String sessionId, Integer code, String reason, Boolean remote){
        this.state = state;
        this.sessionId = sessionId;
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

    public String getSessionId() {
        return sessionId;
    }

    public Boolean isRemote() {
        return remote;
    }
}
