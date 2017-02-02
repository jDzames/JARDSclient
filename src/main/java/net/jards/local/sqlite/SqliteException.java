package net.jards.local.sqlite;

import net.jards.errors.LocalStorageException;

/**
 * Created by jDzama on 28.1.2017.
 */
public class SqliteException extends LocalStorageException {

    private int id;
    private String source;
    private String message;

    public SqliteException(int id, String source, String message){
        this.id = id;
        this.source = source;
        this.message = message;
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
}
