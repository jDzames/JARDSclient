package net.jards.local.sqlite;

import net.jards.core.LocalStorage;
import net.jards.core.Query;
import net.jards.core.StorageSetup;

import java.sql.Connection;
import java.sql.DriverManager;

public class SQLiteLocalStorage extends LocalStorage {


	public SQLiteLocalStorage(StorageSetup storageSetup) {
		super(storageSetup);
		// TODO create db, tables..?
	}

	public void connectDB(String adress){
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");
        } catch ( Exception e ) {
            System.out.println("local db connection error");
            new SqliteError();
        }
        System.out.println("Opened database successfully");
    }

	//tests only
	public void execute(String update){

    }

	public void addCollection(String collection){

    }

    @Override
    public void removeCollection(String collection) {

    }

    public String insert(){
        //TODO return String which can be used in local database?
        return null;
    }

    public String update(){

        return null;
    }

    public String remove(){

        return null;
    }

    public String find(Query query){

        return null;
    }

}

/*
* tu realne dopyty (sql)
*
* */