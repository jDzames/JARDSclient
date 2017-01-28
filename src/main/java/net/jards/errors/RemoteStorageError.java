package net.jards.errors;

/**
 * Created by jDzama on 8.1.2017.
 */
public interface RemoteStorageError {

    int id();
    String source();
    String message();

}
