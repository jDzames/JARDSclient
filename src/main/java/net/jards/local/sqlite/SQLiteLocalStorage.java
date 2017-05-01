package net.jards.local.sqlite;

import net.jards.core.*;
import net.jards.errors.LocalStorageException;

import java.sql.Connection;
import java.sql.*;
import java.sql.ResultSet;
import java.util.*;

/**
 * Implementation of LocalStorage using SQLite as local database.
 * Supports indexes and predicates that use indexed fields.
 */
public class SQLiteLocalStorage extends LocalStorage {

    /**
     * Java sql connection, used to connect to database.
     */
    private Connection connection;
    /**
     * database address
     */
    private final String localDbAddress; //"jdbc:sqlite:test.db"

    /**
     * Constructor with StorageSetup and database address.
     * @param storageSetup setup of collection with prefix, collections and indexes
     * @param databaseAddress address used to connect to database
     */
    public SQLiteLocalStorage(StorageSetup storageSetup, String databaseAddress) {
        super(storageSetup);
        localDbAddress = databaseAddress;
    }

    /**
     * Connects to database. Helper method.
     * @throws SqliteException throws exception if there is problem to connect to database
     */
    @Override
    protected void connectDB() throws SqliteException {
        connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(localDbAddress);  //"jdbc:sqlite:test.db");
        } catch ( Exception e ) {
            System.out.println("local db connection error");
            throw new SqliteException(SqliteException.CONNECTION_EXCEPTION,
                    "Sqlite local database",
                    "Can't connect to local database. \n "+e.toString(),
                    e);
        }
    }

    /**
     * Adds collection to database (creates table).
     * @param collection collection to add
     * @throws SqliteException throws exception if there is problem with writes to database
     */
    @Override
    protected void addCollection(CollectionSetup collection) throws SqliteException {
        connectDB();
        Map<String, String> indexesMap = collection.getIndexes();
        List<String> orderedIndexes = collection.getOrderedIndexes();
        //index columns and sql for creatng indexes
        StringBuilder indexesColumns = new StringBuilder();
        StringBuilder createIndexesSql = new StringBuilder();
        for (String index : orderedIndexes) {
            //add column for index (called as way to value in json)
            indexesColumns.append(", ")
                    .append(index)
                    .append(" ")
                    .append(indexesMap.get(index));
            //add index (createDocument index statement)
            createIndexesSql.append("\n create index ")
                    .append(index)
                    .append("_index on ")
                    .append(getPrefix())
                    .append(collection.getName())
                    .append(" (")
                    .append(index)
                    .append(");");
        }

        //sql for creating table
        String sql = new StringBuilder()
                .append("create table ")
                .append(collection.getFullName())
                .append(" (id varchar(36) primary key, collection text, jsondata text")
                .append(indexesColumns)
                .append(");")
                .append(createIndexesSql)
                .toString();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SqliteException(SqliteException.ADDING_COLLECTION_EXCEPTION,
                    "Sqlite local database, collection "+collection.getName(),
                    "Problem adding collection. \n "+e.toString(),
                    e);
        } finally {
            close(statement);
        }
    }

    /**
     * Removes collection (drops table) froms torage
     * @param collection collection to remove
     * @throws SqliteException throws exception if there is problem with writes to database
     */
    @Override
    protected void removeCollection(CollectionSetup collection) throws SqliteException {
        connectDB();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute("drop table if exists "+collection.getFullName()+";");
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.REMOVING_COLLECTION_EXCEPTION,
                    "Sqlite local database, collection "+collection.getName(),
                    "Problem adding collection. \n "+e.toString(),
                    e);
        } finally {
            close(statement);
        }
    }

    /**
     * Helper method for creating documents that extracts values for indexes used by specified collection.
     * @param collectionName name of collection
     * @param jsonData data/content of documen
     * @return list of values of indexed fields
     * @throws SqliteException throws exception if there is problem with writes to database
     */
    private List<String> getIndexesValues(String collectionName, String jsonData) throws SqliteException {
        //set indexes part of createDocument sql string
        CollectionSetup collectionSetup = getCollectionSetup(collectionName);
        List<String> orderedIndexes = collectionSetup.getOrderedIndexes();
        List<String> orderedIndexesValues = new ArrayList<>();
        if (orderedIndexes.size()==0){
            return orderedIndexesValues;
        }
        JSONPropertyExtractor jsonPropertyExtractor = getJsonPropertyExtractor();
        Map<String, Object> orderedIndexesValuesMap;
        try{
             orderedIndexesValuesMap = jsonPropertyExtractor.extractPropertyValues(jsonData, orderedIndexes);
        } catch (Exception e){
            throw new SqliteException(SqliteException.INDEX_FIELDS_EXCEPTION,
                    "Json property extractor, getting index from json",
                    "Document has wrong fields probably. "+e.toString(),
                    e);
        }

        for (String index:orderedIndexes) {
            orderedIndexesValues.add((String)orderedIndexesValuesMap.get(index));
        }
        return orderedIndexesValues;
    }

    /**
     * Writes/inserts document to database using indexes for document's collection.
     * @param collectionName name of collection for document
     * @param document       document to create write to database)
     * @return id of document
     * @throws SqliteException throws exception if there is problem with writes to database
     */
    @Override
    protected String createDocument(String collectionName, Document document) throws SqliteException {
        //connect
        connectDB();
        //get collection setup
        CollectionSetup collectionSetup = getCollectionSetup(collectionName);
        if (collectionSetup == null){
            throw new SqliteException(SqliteException.INSERT_EXCEPTION,
                    "Sqlite local database, createDocument.",
                    "Wrong collection. ",
                    null);
        }
        //create Document sql string
        StringBuilder sql = new StringBuilder()
                .append("insert or replace into ")
                .append(getPrefix())
                .append(collectionName)
                .append(" values(? , ? , ? ");
        //indexes part
        for (int i = 0; i<collectionSetup.getOrderedIndexes().size(); i++){
            sql.append(", ?");
        }
        sql.append(");");
        //indexes values
        List<String> indexValues = getIndexesValues(collectionName, document.getContent());
        //perform createDocument
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql.toString());
            statement.setString(1, document.getId());
            statement.setString(2, collectionName);
            statement.setString(3, document.getContent());
            for (int i =0; i< indexValues.size(); i++){
                statement.setString(4+i, indexValues.get(i));
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SqliteException(SqliteException.INSERT_EXCEPTION,
                    "Sqlite local database, createDocument.",
                    "Problem with createDocument. \n "+e.toString(),
                    e);
        } finally {
            close(statement);
        }
        return document.getId();
    }

    /**
     * Writes update of document into database.
     * @param collectionName name of collection to which document belongs
     * @param document       document you want to update (original id, changed content)
     * @return id of updated document
     * @throws SqliteException throws exception if there is problem with writes to database
     */
    @Override
    protected String updateDocument(String collectionName, Document document) throws SqliteException {
        connectDB();
        //get collection setup
        CollectionSetup collectionSetup = getCollectionSetup(collectionName);
        if (collectionSetup == null){
            throw new SqliteException(SqliteException.INSERT_EXCEPTION,
                    "Sqlite local database, updateDocument.",
                    "Wrong collection. ",
                    null);
        }
        //sql
        StringBuilder sql = new StringBuilder()
                .append("update ")
                .append(getPrefix())
                .append(collectionName)
                .append(" set jsondata= ? ");
        //indexes part
        for (String index:collectionSetup.getOrderedIndexes()){
            sql.append(", ").append(index).append("= ? ");
        }
        sql.append(" where id= ? ;");
        //index values
        List<String> indexValues = getIndexesValues(collectionName, document.getContent());
        //statement
        PreparedStatement statement = null;
        int idx = 1;
        try {
            statement = connection.prepareStatement(sql.toString());
            statement.setString(idx, document.getContent());
            idx++;
            for (String indexValue:indexValues){
                statement.setString(idx, indexValue);
                idx++;
            }
            statement.setString(idx, document.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.UPDATE_EXCEPTION,
                    "Sqlite local database, updateDocument.",
                    "Problem with createDocument. \n "+e.toString(),
                    e);
        } finally {
            close(statement);
        }
        return document.getId();
    }

    /**
     * Removes/deletes document from database.
     * @param collectionName name of collection where selected document belongs
     * @param document       document to remove
     * @return true if document was deleted successfully
     * @throws SqliteException throws exception if there is problem with writes to database
     */
    @Override
    protected boolean removeDocument(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("delete from ")
                .append(getPrefix())
                .append(collectionName)
                .append(" where id= ? ;")
                .toString();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.setString(1, document.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.UPDATE_EXCEPTION,
                    "Sqlite local database, delete.",
                    "Problem with delete. \n "+e.toString(),
                    e);
        } finally {
            close(statement);
        }
        return true;
    }

    private void close(Statement statement){
        try {
            if (statement != null) {
                statement.close();
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Applies changes to local database. Creates new collections if needed, insert new documents, updates edited and removed deleted.
     * @param changes List of DocumentChanges. Can contain collections which does not exist in local database.
     * @throws LocalStorageException throws exception if any of write updates fails
     */
    @Override
    protected void applyDocumentChanges(DocumentChanges changes) throws LocalStorageException {
            for (Document addedDocument:changes.getAddedDocuments()) {
                String collectionName = addedDocument.getCollection().getName();
                if (getCollectionSetup(collectionName) == null){
                    //this table is not in database, create new (server sent document from new collection)
                    CollectionSetup newCollectionSetup = new CollectionSetup(getPrefix(), collectionName, false);
                    addCollectionSetup(newCollectionSetup);
                    removeCollection(newCollectionSetup);
                    addCollection(newCollectionSetup);
                }
                this.createDocument(collectionName, addedDocument);
            }
            for (Document updatedDocument:changes.getUpdatedDocuments()) {
                String collectionName = updatedDocument.getCollection().getName();
                if (getCollectionSetup(collectionName) == null){
                    //this table is not in database, cant edit it's documents
                    //TODO error also?
                    continue;
                }
                this.updateDocument(collectionName, updatedDocument);
            }
            for (Document removedDocument:changes.getRemovedDocuments()) {
                String collectionName = removedDocument.getCollection().getName();
                if (getCollectionSetup(collectionName) == null){
                    //this table is not in database, cant remove it's documents
                    //TODO error also?
                    continue;
                }
                this.removeDocument(collectionName, removedDocument);
            }

    }


    /**
     * Starts local storage work (nothing needed in this implementation).
     * @return saved requests (not implemented yet).
     */
    @Override
    protected List<ExecutionRequest> startLocalStorage() {
        return null;
    }

    /**
     * Stops local storage work (nothing needed in this implementation).
     * @param unconfirmedRequests requests to save (pending requests should be added too probably)
     */
    @Override
    protected void stopLocalStorage(Queue<ExecutionRequest> unconfirmedRequests) {

    }


    /**
     * Finds all documents/table rows for specified predicate.
     * @param collectionName name of collection
     * @param p              predicate to filter result
     * @param options        options for result (ie. order...)
     * @return list with documents (in map representation)
     * @throws SqliteException throws exception if there is problem with reading database
     */
    @Override
    protected List<Map<String, String>> find(String collectionName, Predicate p, ResultOptions options) throws SqliteException {
        connectDB();
        PreparedStatement preparedStatement = null;
        try {
            CollectionSetup collectionSetup = getCollectionSetup(collectionName);
            if (collectionSetup == null){
                throw new SqliteException(SqliteException.QUERY_EXCEPTION,
                        "Sqlite local database, preparing query.",
                        "Wrong collection. ", null);
            }
            if (options == null){
                //add empty options
                options = new ResultOptions();
            }
            //prepare statement and fill it with parameters
            SQLiteQueryGenerator sqLiteQueryGenerator = new SQLiteQueryGenerator(collectionSetup);
            Predicate supportedPredicate = LocalStorage.createFilteringPredicate(p, new PredicateFilter() {
                @Override
                public boolean isAcceptable(Predicate predicate) {
                    return sqLiteQueryGenerator.isAcceptable(p);
                }
            });
            preparedStatement = sqLiteQueryGenerator.generateFilterStatement(connection, supportedPredicate, options);
            //execute query
            ResultSet rs;
            rs = preparedStatement.executeQuery();
            List<Map<String, String>> foundDocuments = new LinkedList<>();
            //fill List and return it
            int  documentCounter = options.getLimit()>0 ? options.getLimit() : Integer.MAX_VALUE;
            while(rs.next() && documentCounter>0)
            {
                // add one row (document)
                Map<String, String> documentMap = new HashMap<>();
                documentMap.put("id", rs.getString("id"));
                documentMap.put("collection", rs.getString("collection"));
                documentMap.put("jsondata", rs.getString("jsondata"));
                foundDocuments.add(documentMap);
                documentCounter--;
            }
            // return data to storage
            return foundDocuments;
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.QUERY_EXCEPTION,
                    "Sqlite local database, executing query.",
                    "Problem with executing query. \n "+e.toString(),
                    e);
        } finally {
            close(preparedStatement);
        }
    }

    /**
     * Finds documents specified by predicate and returns first one.
     * @param collectionName name of collection
     * @param p              predicate to filter result
     * @param options        options for result (ie. order...)
     * @return map representation of first document in result
     * @throws SqliteException throws exception if there is problem with reading database
     */
    @Override
    protected Map<String, String> findOne(String collectionName, Predicate p, ResultOptions options) throws SqliteException {
        if (options==null){
            options = new ResultOptions();
        }
        options.setLimit(1);
        List<Map<String, String>> result = find(collectionName, p, options);
        if (result == null || result.size() == 0){
            return null;
        }
        return result.get(0);
    }

}