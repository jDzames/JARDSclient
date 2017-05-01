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
            //find one simple example
            Document najdeny = collection.findOne();
            String porovnavanePole = "text";
            String hodnota = "text niektoreho dokumentu";
            Predicate podmienka = new Predicate.Equals(porovnavanePole, hodnota);
            Document najdenyDocument = collection.findOne(podmienka);


            //find all with changes
            ResultSet resultSet = collection.find(null, null);
            //System.out.println(resultSet.getDocuments().toString());
            DocumentList zoznamDokumentov = resultSet.getDocuments();
            //get with listener notified on changes
            resultSet.addActualDocumentsListener(new ResultSet.ActualDocumentsListener() {
                @Override
                public void resultChanged(DocumentList actualDocuments) {
                    System.out.println("Result set changed: "+actualDocuments.toString());
                }
            });
            //get as rx changes
            resultSet.getAsRxList()
                    .subscribe(actualList ->
                            System.out.println("RX " + actualList.toString()));

        } catch (LocalStorageException e) {
            e.printStackTrace();
        }
    }
}
