package net.jards.local.sqlite;

import net.jards.core.Document;
import net.jards.core.LocalStorage;
import net.jards.core.Query;
import net.jards.core.StorageSetup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteLocalStorage extends LocalStorage {


    private Connection connection;
    private final String localDbAdress;

    public SQLiteLocalStorage(StorageSetup storageSetup) {
		super(storageSetup);
		// TODO create db, tables..?
        localDbAdress = ""; // storagesetup
	}

	private void connectDB() throws SqliteException {
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


	public void addCollection(String collectionName) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("create table")
                .append(collectionName)
                .append(" (id varchar(36) primary key, collection text, jsondata text);")
                .toString();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw new SqliteException(SqliteException.ADDING_COLLECTION_EXCEPTION,
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
    public void removeCollection(String collectionName) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("drop table")
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

    public String insert(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("insert into ").append(collectionName)
                .append(" values( '")
                .append(document.getUuid()).append("', '")
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
        return document.getUuid().toString();
    }

    public String update(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("update ").append(collectionName)
                .append(" set jsondata='").append(document.getJsonData())
                .append("' where id='").append(document.getUuid()).append("';")
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
        return document.getUuid().toString();
    }

    public boolean remove(String collectionName, Document document) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("delete from ").append(collectionName)
                .append(" where id='").append(document.getUuid()).append("';")
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

    public String find(Query query) throws SqliteException {
        connectDB();
        String sql = new StringBuilder()
                .append("select * from "+query.getCollection()).append(";")
                .toString();

        return null;
    }

}

/*
* tu realne dopyty (sql)
*
* */