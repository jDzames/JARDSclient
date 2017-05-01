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
        //creating app
        StorageSetup storageSetup = new StorageSetup();
        storageSetup.setPrefix("tests_");
        storageSetup.addCollectionSetup("LocalTest", true);
        storageSetup.addCollectionSetup("tasks", false);
        DDPConnectionSettings connectionSettings = new DDPConnectionSettings("localhost",
                3000, Username, "testik", "testik");
        RemoteStorage remoteStorage = new DDPRemoteStorage(storageSetup, connectionSettings);
        LocalStorage localStorage;
        Storage storage = null;
        try {
            localStorage = new SQLiteLocalStorage(storageSetup, "jdbc:sqlite:test.db");
            storage = new Storage(storageSetup, remoteStorage, localStorage);

            //start - all 3 storages starts
            storage.start("");

            //register speculative method for local execution
            storage.registerSpeculativeMethod("tasks.insertWithSeed", new TransactionRunnableExecutions());

            //execute method with query
            storage.execute(new TransactionRunnableQuery());

            storage.subscribe("tasks");
            //storage.call("tasks.insertWithSeed", "{\"text\":\"for seed test\"}");
            //storage.executeAsync(new TransactionRunnableExecutions());

            /* use case testing code
            TransactionRunnable example = (context, transaction, arguments) -> {
                Collection tasks = context.getCollection("tasks");           //gets collection "tasks"
                Document document = new Document((String)arguments[0]);      //prepares document
                document = tasks.create(document, transaction);              //writes document
                ResultSet result = tasks.find();                             //gets all documents from collection
                result.getAsRxList().subscribe(actualResult ->
                        System.out.println(actualResult.toString()));        //get as RxJava Observable
            };
            storage.registerSpeculativeMethod("tasks.insert", example);
            storage.call("tasks.insert", "Mount Everest");    //call method on server (execute speculation)

            storage.executeAsync(example, "Mount Blanc");   //or execute locally and send changes to server
            */

            //wait for work to finish (and see logs in console)
            Thread.sleep(3000);
            //Thread.sleep(300000);
            storage.stop();

        } catch (LocalStorageException | InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue( true );
    }
}