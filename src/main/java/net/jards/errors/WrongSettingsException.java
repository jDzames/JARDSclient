package net.jards.errors;

/**
 * Originally used to tell user about IllegalArguments/NullPointer. Not used anymore.
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
