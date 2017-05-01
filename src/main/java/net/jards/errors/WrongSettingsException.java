package net.jards.errors;

/**
 * Created by jDzama on 24.4.2017.
 */
public class WrongSettingsException extends Exception {

    private String source;
    private String message;

    public WrongSettingsException(String source, String message){
        this.source = source;
        this.message = message;
    }

    public String source() {
        return this.source;
    }

    public String message() {
        return this.message;
    }

}
