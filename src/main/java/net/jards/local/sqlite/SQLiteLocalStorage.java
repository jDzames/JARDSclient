package net.jards.local.sqlite;

import net.jards.core.*;

import java.sql.Connection;
import java.sql.*;
import java.sql.ResultSet;
import java.util.*;

public class SQLiteLocalStorage extends LocalStorage {


    private Connection connection;
    private final String localDbAdress;

    public SQLiteLocalStorage(StorageSetup storageSetup) {
		super(storageSetup);
		// TODO create db, tables..?
        localDbAdress = ""; // storagesetup
	}

    @Override
    public List<ExecutionRequest> start() {
        //TODO read requests from collection for them
        return null;
    }

    @Override
    public void stop(Queue<ExecutionRequest> unconfirmedRequests) {
        //TODO save requests into collection for them
    }

    @Override
    public void connectDB() throws SqliteException {
        connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:test.db");
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
        //index columns and sql for creatng indexes
        StringBuilder indexesColumns = new StringBuilder();
        StringBuilder createIndexesSql = new StringBuilder();
        for (String index : indexesMap.keySet()) {
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
    public void removeCollection(String collectionName) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("drop table")
                .append(getTablePrefix())
                .append(collectionName)
                .append(";")
                .toString();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.REMOVING_COLLECTION_EXCEPTION,
                    "Sqlite local database, collection "+collectionName,
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
    public String insert(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("insert into ")
                .append(getTablePrefix())
                .append(collectionName)
                .append(" values( '")
                .append(document.getId()).append("', '")
                .append(document.getCollection()).append("', '")
                .append(document.getJsonData()).append("');")
                .toString();
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
        return document.getId().toString();
    }

    @Override
    public String update(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("update ")
                .append(getTablePrefix())
                .append(collectionName)
                .append(" set jsondata='").append(document.getJsonData())
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
        return document.getId().toString();
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