package net.jards.errors;

/**
 * Originally exception used in transaction check.
 */
public class TransactionException extends RuntimeException {

    private String source;
    private String message;

    public TransactionException(String source, String message){
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
