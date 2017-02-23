package net.jards.local.sqlite;

import net.jards.errors.LocalStorageException;

/**
 * Created by jDzama on 28.1.2017.
 */
public class SqliteException extends LocalStorageException {

    private int id;
    private String source;
    private String message;
    private Exception innerException;

    public SqliteException(int id, String source, String message, Exception innerException){
        this.id = id;
        this.source = source;
        this.message = message;
        this.innerException = innerException;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public String source() {
        return this.source;
    }

    @Override
    public String message() {
        return this.message;
    }

    @Override
    public Exception innerException() {
        return this.innerException;
    }
}
