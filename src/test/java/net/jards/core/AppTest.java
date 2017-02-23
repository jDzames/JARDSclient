package net.jards.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jards.errors.LocalStorageException;
import net.jards.local.sqlite.SQLiteLocalStorage;
import net.jards.remote.ddp.DDPConnectionSettings;
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


    public void testApp()
    {

        StorageSetup storageSetup = new StorageSetup();
        storageSetup.setPrefix("tests_");
        storageSetup.addCollectionSetup("LocalTest", true, "example");
        DDPConnectionSettings connectionSettings = new DDPConnectionSettings("localhost", 3000, DDPConnectionSettings.LoginType.Username, "testik", "testik");
        RemoteStorage remoteStorage = new DDPRemoteStorage(storageSetup, connectionSettings);
        LocalStorage localStorage = null;
        try {
            localStorage = new SQLiteLocalStorage(storageSetup, "jdbc:sqlite:test.db");
        } catch (LocalStorageException e) {
            e.printStackTrace();
            //System.out.println(e.message());
            //assertNotNull(localStorage);
            return;
        }

        Storage storage = new Storage(storageSetup, remoteStorage, localStorage);
        storage.start("");


        //storage.subscribe("tasks");

        /*Object[] methodArgs = new Object[1];
        methodArgs[0] = "Pridany cez DDP 2";*/
		//storage.callAsync("tasks.createDocument", "Pridany cez DDP 3");
        //storage.executeAsync(new TransactionRunnableTest());

        storage.executeLocallyAsync(new TransactionRunnableTest());



        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue( true );
    }
}








/*
            Object[] methodArgs = new Object[1];
			UsernameAuth auth = new UsernameAuth("testik", "testik");
			methodArgs[0] = auth;
			int methodId = ddp.call("login", methodArgs, obs);
			System.out.println("Login id  =  "+methodId);


			Thread.sleep(1000);
			//System.out.println(obs.mCollections.toString());
			methodArgs = new Object[1];
			methodArgs[0] = "Pridany cez Javuuuuu";
			int callId = ddp.call("tasks.createDocument", methodArgs);
			System.out.println("Call id  =  "+callId);
*/
/**
 * Rigourous Test :-)
 */