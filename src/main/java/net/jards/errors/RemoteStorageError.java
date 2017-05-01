package net.jards.errors;

/**
 * Error in remotes storage used in RemoteStorageListener.
 */
public interface RemoteStorageError {

    int id();
    String source();
    String message();

}
