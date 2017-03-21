package net.jards.errors;

/**
 * Created by jDzama on 21.3.2017.
 */
public class JsonFormatException extends Exception {

    private final String message;
    private final Exception innerException;

    public JsonFormatException(String message, Exception innerException){
        this.message = message;
        this.innerException = innerException;
    }

    public String message() {
        return message;
    }

    public Exception innerException() {
        return innerException;
    }
}
