package net.jards.errors;

/**
 * Created by jDzama on 28.1.2017.
 */
public abstract class LocalStorageException extends Exception{


    public static final int CONNECTION_EXCEPTION = 1;
    public static final int ADDING_COLLECTION_EXCEPTION = 2;
    public static final int REMOVING_COLLECTION_EXCEPTION = 3;
    public static final int INSERT_EXCEPTION = 4;
    public static final int UPDATE_EXCEPTION = 5;


    private int id;
    private String source;
    private String message;


    public abstract int id();
    public abstract String source();
    public abstract String message();

}
