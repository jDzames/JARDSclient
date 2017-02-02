package net.jards.remote.ddp;

import net.jards.errors.RemoteStorageError;

/**
 * Created by jDzama on 8.1.2017.
 */
public class DefaultRemoteStorageError implements RemoteStorageError {

    private final String message;
    private final String source;
    private int id;

    public DefaultRemoteStorageError(int id, String source, String message){
        this.id = id;
        this.message = message;
        this.source = source;
    }


    @Override
    public int id() {
        return this.id;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public String message() {
        return message();
    }
}
