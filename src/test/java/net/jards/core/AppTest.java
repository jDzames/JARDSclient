package net.jards.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jards.errors.LocalStorageException;
import net.jards.local.sqlite.SQLiteLocalStorage;
import net.jards.remote.ddp.DDPConnectionSettings;
import net.jards.remote.ddp.DDPRemoteStorage;

import static net.jards.remote.ddp.DDPConnectionSettings.LoginType.Username;

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
        storageSetup.addCollectionSetup("tasks", false);
        DDPConnectionSettings connectionSettings = new DDPConnectionSettings("localhost", 3000, Username, "testik", "testik");
        RemoteStorage remoteStorage = new DDPRemoteStorage(storageSetup, connectionSettings);
        LocalStorage localStorage;
        Storage storage = null;
        try {
            localStorage = new SQLiteLocalStorage(storageSetup, "jdbc:sqlite:test.db");
            storage = new Storage(storageSetup, remoteStorage, localStorage);

            storage.start("");

            Thread.sleep(3000);


            //storage.subscribe("tasks");

            /*Object[] methodArgs = new Object[1];
            methodArgs[0] = "Pridany cez DDP 2";*/
            //storage.callAsync("tasks.insert", "Pridany cez DDP 4");




            //wait for work to finish (and see logs in console)
            Thread.sleep(3000);
            //Thread.sleep(12000);

            storage.executeLocallyAsync(new TransactionRunnableTest());

            //Thread.sleep(300000);
            Thread.sleep(12000);

        } catch (LocalStorageException | InterruptedException e) {
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