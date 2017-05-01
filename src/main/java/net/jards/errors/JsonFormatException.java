package net.jards.errors;

/**
 * Exception used in JSONPropertyExtractor.
 */
public class JsonFormatException extends Exception {

    private final String message;
    private final Exception innerException;

    public JsonFormatException(String message, Exception innerException){
        this.message = message;
        this.innerException = innerException;
    }

    /**
     * @return  message of this exception
     */
    public String message() {
        return message;
    }

    /**
     * @return inner exception that caused this exception to be thrown
     */
    public Exception innerException() {
        return innerException;
    }
}
