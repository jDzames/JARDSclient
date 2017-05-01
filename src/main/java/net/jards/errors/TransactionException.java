package net.jards.errors;

/**
 * Created by jDzama on 30.4.2017.
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
