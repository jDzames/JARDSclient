package net.jards.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jards.local.sqlite.SQLiteLocalStorage;
import net.jards.remote.ddp.DDPRemoteStorage;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {

        StorageSetup storageSetup = new StorageSetup();
        RemoteStorage remoteStorage = new DDPRemoteStorage(storageSetup, "localhost/meteor");
        LocalStorage localStorage = new SQLiteLocalStorage(storageSetup);
        Storage storage = new Storage(storageSetup, remoteStorage, localStorage);

        assertTrue( true );
    }
}
