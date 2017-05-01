package net.jards.errors;

/**
 * Exception used in query execution.
 */
public class QueryException extends LocalStorageException {

    private int id;
    private String source;
    private String message;
    private Exception innerException;

    public QueryException(int id, String source, String message, Exception exception){
        this.id = id;
        this.source = source;
        this.message = message;
        this.innerException = exception;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Exception innerException() {
        return this.innerException;
    }
}
