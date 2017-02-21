package net.jards.local.sqlite;

import net.jards.core.*;
import net.jards.errors.LocalStorageException;

import java.sql.Connection;
import java.sql.*;
import java.sql.ResultSet;
import java.util.*;

public class SQLiteLocalStorage extends LocalStorage {


    private Connection connection;
    private final String localDbAdress; //"jdbc:sqlite:test.db"

    public SQLiteLocalStorage(StorageSetup storageSetup, String databaseConnection) throws LocalStorageException {
		super(storageSetup);
		// TODO create db, tables..?
        localDbAdress = databaseConnection;
	}

    @Override
    public List<ExecutionRequest> start() {
        //TODO read requests from collection for them. move and do in default LocalStorage or user here?
        return null;
    }

    @Override
    public void stop(Queue<ExecutionRequest> unconfirmedRequests) {
        //TODO save requests into collection for them. same question.
    }

    @Override
    public void connectDB() throws SqliteException {
        connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(localDbAdress);
        } catch ( Exception e ) {
            System.out.println("local db connection error");
            //TODO error finish (message..)
            throw new SqliteException(SqliteException.CONNECTION_EXCEPTION, "Sqlite local database", "Can't connect to local database. \n "+e.toString());
        }
    }

    @Override
	public void addCollection(CollectionSetup collection) throws SqliteException {
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
            //add index (create index statement)
            createIndexesSql.append("\n create index ")
                    .append(index)
                    .append("_index on ")
                    .append(collection.getFullName())
                    .append(" (")
                    .append(index)
                    .append(");");
        }

        //sql for creating table
        String sql = new StringBuilder()
                .append("create table")
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
            throw new SqliteException(SqliteException.ADDING_COLLECTION_EXCEPTION,
                    "Sqlite local database, collection "+collection.getName(),
                    "Problem adding collection. \n "+e.toString());
        } finally {
            try {
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void removeCollection(CollectionSetup collection) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("drop table if exists")
                .append(collection.getFullName())
                .append(";")
                .toString();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.REMOVING_COLLECTION_EXCEPTION,
                    "Sqlite local database, collection "+collection.getName(),
                    "Problem adding collection. \n "+e.toString());
        } finally {
            try {
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private StringBuilder createInsertIndexPartSql(String collectionName, String jsonData){
        //set indexes part of insert sql string
        CollectionSetup collectionSetup = getCollectionSetup(collectionName);
        List<String> orderedIndexes = collectionSetup.getOrderedIndexes();
        JSONPropertyExtractor jsonPropertyExtractor = getJsonPropertyExtractor();
        Map<String, Object> orderedIndexesValues = jsonPropertyExtractor.extractPropertyValues(jsonData, orderedIndexes);
        StringBuilder indexesSqlPart = new StringBuilder();
        for (String index:orderedIndexes) {
            indexesSqlPart.append(", ").append((String)orderedIndexesValues.get(index));
        }
        return indexesSqlPart;
    }

    @Override
    public String insert(String collectionName, Document document) throws SqliteException {
        //connect
        connectDB();
        //insert sql string
        String sql = new StringBuilder()
                .append("insert into ")
                .append(getTablePrefix())
                .append(collectionName)
                .append(" values( '")
                .append(document.getId()).append("', '")
                .append(document.getCollection()).append("', '")
                .append(document.getJsonData()).append("'")
                .append(createInsertIndexPartSql(collectionName, document.getJsonData()))
                .append(");")
                .toString();
        //perform insert
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.INSERT_EXCEPTION,
                    "Sqlite local database, insert.",
                    "Problem with insert. \n "+e.toString());
        } finally {
            try {
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return document.getId();
    }

    private StringBuilder createUpdateIndexPartSql(String collectionName, String jsonData){
        //set indexes part of update sql string
        CollectionSetup collectionSetup = getCollectionSetup(collectionName);
        List<String> orderedIndexes = collectionSetup.getOrderedIndexes();
        JSONPropertyExtractor jsonPropertyExtractor = getJsonPropertyExtractor();
        Map<String, Object> orderedIndexesValues = jsonPropertyExtractor.extractPropertyValues(jsonData, orderedIndexes);
        StringBuilder indexesSqlPart = new StringBuilder();
        for (String index:orderedIndexes) {
            indexesSqlPart.append(", ").append(index).append("=").append((String)orderedIndexesValues.get(index));
        }
        return indexesSqlPart;
    }

    @Override
    public String update(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("update ")
                .append(getTablePrefix())
                .append(collectionName)
                .append(" set jsondata='").append(document.getJsonData())
                .append(createUpdateIndexPartSql(collectionName, document.getJsonData()))
                .append("' where id='").append(document.getId()).append("';")
                .toString();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.UPDATE_EXCEPTION,
                    "Sqlite local database, update.",
                    "Problem with insert. \n "+e.toString());
        } finally {
            try {
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return document.getId();
    }

    @Override
    public boolean remove(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("delete from ")
                .append(getTablePrefix())
                .append(collectionName)
                .append(" where id='")
                .append(document.getId())
                .append("';")
                .toString();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.UPDATE_EXCEPTION,
                    "Sqlite local database, delete.",
                    "Problem with delete. \n "+e.toString());
        } finally {
            try {
                statement.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public List<Map<String, String>> find(Query query) throws SqliteException {
        connectDB();
        String sql;
        if (query.isRawQuery()){
            sql = query.getRawSql();
        } else {
            sql = new StringBuilder()
                    .append("select * from ")
                    .append(query.getCollection())
                    .append(" ")
                    .append(query.getWhere())
                    .append(";")
                    .toString();
        }
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.createStatement();
            rs = statement.executeQuery(sql);
            List<Map<String, String>> foundDocuments = new LinkedList<>();
            while(rs.next())
            {
                // add one row (document)
                Map<String, String> documentMap = new HashMap<>();
                documentMap.put("id", rs.getString("id"));
                documentMap.put("collection", rs.getString("collection"));
                documentMap.put("jsondata", rs.getString("jsondata"));
                foundDocuments.add(documentMap);
            }
            // return data to storage
            return foundDocuments;
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.QUERY_EXCEPTION,
                    "Sqlite local database, executing query.",
                    "Problem with executing query. \n "+e.toString());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (rs != null) {
                    rs.close();
                }
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, String> findOne(Query query) throws SqliteException {
        return null;
    }

}