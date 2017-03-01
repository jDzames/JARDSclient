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
    public static final int QUERY_BUILDING_EXCEPTION = 6;
    public static final int QUERY_EXCEPTION = 7;
    public static final int SETUP_EXCEPTION = 8;
    public static final int INDEX_FIELDS_EXCEPTION = 9;

    private int id;
    private String source;
    private String message;
    private Exception innerException;


    public abstract int id();
    public abstract String source();
    public abstract String message();
    public abstract Exception innerException();

}
