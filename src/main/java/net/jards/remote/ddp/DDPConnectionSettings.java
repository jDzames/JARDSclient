package net.jards.remote.ddp;

/**
 * Created by jDzama on 17.1.2017.
 */
public class DDPConnectionSettings{

    public enum LoginType {
        Username,
        Email,
        Token,
        NoLogin
    }

    private final String serverAdress;
    private final int serverPort;
    private final LoginType loginType;
    private final String userName;
    private final String email;
    private final String password;
    private final String resumeToken;

    public DDPConnectionSettings(String serverAdress, int serverPort) {
        this.serverAdress = serverAdress;
        this.serverPort = serverPort;
        this.userName = null;
        this.email = null;
        this.password = null;
        this.resumeToken = null;
        this.loginType = LoginType.NoLogin;
    }

    public DDPConnectionSettings(String serverAdress, int serverPort, LoginType loginType, String login, String password) {
        this.serverAdress = serverAdress;
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

    public DDPConnectionSettings(String serverAdress, int serverPort, String resumeToken) {
        this.serverAdress = serverAdress;
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

    public String getServerAdress() {
        return serverAdress;
    }

    public String getUserName() {
        return userName;
    }

    public LoginType getLoginType() {
        return loginType;
    }
}
