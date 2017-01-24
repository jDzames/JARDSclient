package net.jards.local.sqlite;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by jDzama on 21.1.2017.
 */
public class SQLiteTest {

    public static void main(String[] args) {
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

}
