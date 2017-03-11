package net.jards.core;

import net.jards.errors.LocalStorageException;

/**
 * Created by jDzama on 11.1.2017.
 */
public class TransactionRunnableTest implements  TransactionRunnable {

    @Override
    public void run(ExecutionContext context, Transaction transaction, Object... arguments) {
        Document d = new Document();
        d.setJsonData("{example:for local}");//"{test1:Added through StoragesTogether}");
        Collection collection = context.getCollection("LocalTest");
        try {
            d = collection.create(d, transaction);
            //d.setJsonData("{example:changedData}");
            //d = collection.update(d, transaction);
            //System.out.println(d.getJsonData());
            //collection.remove(d, transaction);
            System.out.println(collection.findOne(null, null).getJsonData());
        } catch (LocalStorageException e) {
            e.printStackTrace();
        }
        //System.out.println(d.getId());
    }

}
