package net.jards.core;

import java.util.UUID;

/**
 * Created by jDzama on 11.1.2017.
 */
public class TransactionRunnableTest implements  TransactionRunnable {

    @Override
    public void run(ExecutionContext context, Transaction transaction, Object... arguments) {
        Document d = new Document(context.getCollection("example"), UUID.randomUUID());
        d.setJsonData("example:data");

    }

}
