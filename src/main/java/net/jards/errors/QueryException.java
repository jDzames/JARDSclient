package net.jards.errors;

/**
 * Created by jDzama on 8.2.2017.
 */
public class QueryException extends LocalStorageException {

    private int id;
    private String source;
    private String message;

    public QueryException(int id, String source, String message){
        this.id = id;
        this.source = source;
        this.message = message;
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
}
