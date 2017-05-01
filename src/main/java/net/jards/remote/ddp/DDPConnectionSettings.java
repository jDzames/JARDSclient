package net.jards.remote.ddp;

/**
 * Class which provides information and settings for connecting to server.
 */
public class DDPConnectionSettings{

    /**
     * Enum for DDP login types.
     */
    public enum LoginType {
        Username,
        Email,
        Token,
        NoLogin
    }

    /**
     * server address
     */
    private final String serverAddress;
    /**
     * port on which server listens
     */
    private final int serverPort;
    /**
     *DDP login type (username, email, token or no login)
     */
    private final LoginType loginType;
    /**
     * username for corresponding login type
     */
    private final String userName;
    /**
     *email for for corresponding login type
     */
    private final String email;
    /**
     * password for login (user and email types)
     */
    private final String password;
    /**
     * token for corresponding login type
     */
    private final String resumeToken;

    /**
     * Creates DDPConnectionSettings with server adress and port
     * @param serverAddress address to server
     * @param serverPort port where server listens
     */
    public DDPConnectionSettings(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.userName = null;
        this.email = null;
        this.password = null;
        this.resumeToken = null;
        this.loginType = LoginType.NoLogin;
    }

    /** Consctructor providing server adress, port, RemoteLoginType which can be username or email and login and password
     * @param serverAddress adress of server
     * @param serverPort server port
     * @param loginType login type - this constructor accepts username or password
     * @param login login String
     * @param password possword to login
     */
    public DDPConnectionSettings(String serverAddress, int serverPort, LoginType loginType, String login, String password) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        if(loginType == LoginType.Username) {
            this.userName = login;
            this.email = null;
        } else if(loginType == LoginType.Email){
            this.userName = null;
            this.email = login;
        } else {
            //TODO error
            this.userName = null;
            this.email = null;
        }
        this.password = password;
        this.resumeToken = null;
        this.loginType = loginType;
    }

    /**
     * Constructer which uses token for login.
     * @param serverAddress address of server
     * @param serverPort server port
     * @param resumeToken token for login
     */
    public DDPConnectionSettings(String serverAddress, int serverPort, String resumeToken) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.userName = null;
        this.email = null;
        this.password = null;
        this.resumeToken = resumeToken;
        this.loginType = LoginType.Token;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getResumeToken() {
        return resumeToken;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getUserName() {
        return userName;
    }

    public LoginType getLoginType() {
        return loginType;
    }
}
