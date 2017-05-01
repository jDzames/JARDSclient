package net.jards.local.sqlite;

import net.jards.errors.LocalStorageException;

/**
 * Exception for SQLite implementation of LocalStorage.
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

    /**
     * @return id of this exception
     */
    @Override
    public int id() {
        return this.id;
    }

    /**
     * @return source of this exception
     */
    @Override
    public String source() {
        return this.source;
    }

    /**
     * @return message of this exception
     */
    @Override
    public String message() {
        return this.message;
    }

    /**
     * @return exception that caused this exception
     */
    @Override
    public Exception innerException() {
        return this.innerException;
    }
}
