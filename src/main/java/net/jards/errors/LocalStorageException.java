package net.jards.errors;

/**
 * Exception used in LocalStorage.
 */
public abstract class LocalStorageException extends Exception{


    /**
     * problem with connection
     */
    public static final int CONNECTION_EXCEPTION = 1;
    /**
     * problem to add colelction
     */
    public static final int ADDING_COLLECTION_EXCEPTION = 2;
    /**
     * problem problem removing collection
     */
    public static final int REMOVING_COLLECTION_EXCEPTION = 3;
    /**
     * problem while creating document
     */
    public static final int INSERT_EXCEPTION = 4;
    /**
     * problem while updating document
     */
    public static final int UPDATE_EXCEPTION = 5;
    /**
     * problem while building query
     */
    public static final int QUERY_BUILDING_EXCEPTION = 6;
    /**
     * problem while executing query
     */
    public static final int QUERY_EXCEPTION = 7;
    /**
     * problem while setting up storage
     */
    public static final int SETUP_EXCEPTION = 8;
    /**
     * problem with index fields
     */
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
