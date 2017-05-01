package net.jards.core;

import net.jards.errors.LocalStorageException;


public class TransactionRunnableExecutions implements  TransactionRunnable {

    @Override
    public void run(ExecutionContext context, Transaction transaction, Object... arguments) {
        //get argument value
        String value = (String) arguments[0];
        //create and create document
        Document d = new Document();
        d.setContent(value); //("{\"text\":\"simple test\"}");

        //more documents for query predicates tests
        /*Document d1 = new Document();
        d1.setContent("{example:five,not_example:ok}");//"{test1:Added through StoragesTogether}");
        Document d2 = new Document();
        d2.setContent("{example:six,not_example:ok}");//"{test1:Added through StoragesTogether}");*/

        //get collection
        Collection collection = context.getCollection("tasks");
        try {
            d = collection.create(d, transaction);
            //d1 = collection.create(d1, transaction);
            //d2 = collection.create(d2, transaction);
            //d.setContent("{example:changedData}");
            //d = collection.update(d, transaction);
            //collection.remove(d, transaction);
            /*collection.find(new Predicate.Equals("example","six")).getAsRxList()
                    .subscribe(documents -> System.out.println(documents.toString()));*/
        } catch (LocalStorageException e) {
            e.printStackTrace();
        }
        //System.out.println(d.getId());
    }

}
