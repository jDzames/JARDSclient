package net.jards.core;

import net.jards.errors.LocalStorageException;

/**
 * Created by jDzama on 11.1.2017.
 */
public class TransactionRunnableExecutions implements  TransactionRunnable {

    @Override
    public void run(ExecutionContext context, Transaction transaction, Object... arguments) {
        Document d = new Document();
        d.setJsonData("{\"text\":\"for test\"}");//"{test1:Added through StoragesTogether}");
        /*Document d1 = new Document();
        d1.setJsonData("{example:five,not_example:ok}");//"{test1:Added through StoragesTogether}");
        Document d2 = new Document();
        d2.setJsonData("{example:six,not_example:ok}");//"{test1:Added through StoragesTogether}");*/

        System.out.println("executing speculation");
        Collection collection = context.getCollection("tasks");
        try {
            d = collection.create(d, transaction);
            //d1 = collection.create(d1, transaction);
            //d2 = collection.create(d2, transaction);
            //d.setJsonData("{example:changedData}");
            //d = collection.update(d, transaction);
            //System.out.println(d.getJsonData());
            //collection.remove(d, transaction);
            System.out.println("        id       "+d.getId());
        } catch (LocalStorageException e) {
            e.printStackTrace();
        }
        //System.out.println(d.getId());
    }

}
