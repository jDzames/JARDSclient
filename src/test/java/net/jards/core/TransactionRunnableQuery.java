package net.jards.core;

import net.jards.errors.LocalStorageException;

/**
 * Created by jDzama on 14.3.2017.
 */
public class TransactionRunnableQuery implements TransactionRunnable {
    @Override
    public void run(ExecutionContext context, Transaction transaction, Object... arguments) {

        Collection collection = context.getCollection("tasks");
        try {
            ResultSet resultSet = collection.find(null, null);
            //System.out.println(resultSet.getDocuments().toString());
            System.out.println("query running");
            resultSet.addActualDocumentsListener(new ResultSet.ActualDocumentsListener() {
                @Override
                public void resultChanged(DocumentList actualDocuments) {
                    System.out.println("Result set changed: "+actualDocuments.toString());
                }
            });

        } catch (LocalStorageException e) {
            e.printStackTrace();
        }
    }
}
