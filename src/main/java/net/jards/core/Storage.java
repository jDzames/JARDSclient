package net.jards.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Transparent storage implementation that combines data from local and remote
 * storage. The implementation is thread-safe.
 */
public class Storage {

	private class RequestHandleThread extends Thread {
		@Override
		public void run() {

			//Get this thread, so program can check and dont allow transactions in another.
			setThreadForLocalDBRuns(Thread.currentThread());

			while (true) {
				ExecutionRequest request = null;
				synchronized (pendingRequests) {
                    while (pendingRequests.isEmpty()){
                        try {
                            pendingRequests.wait();
					    } catch (InterruptedException e) {
						    e.printStackTrace();
					    }
                    }
					request = pendingRequests.poll();
				}
				if (request == null){
					//TODO
					continue;
				}
                System.out.println("POSIELAM INSERT 3");
				ExecutionContext context = request.getContext();
				Transaction transaction = request.getTransaction();
				Object[] arguments = request.getAttributes();
                String methodName = request.getMethodName();
				TransactionRunnable runnable = request.getRunnable();
				runnable.run(context, transaction, arguments);

				if (request.isLocal()){
                    //only local execution

				} else if (methodName == null || methodName == ""){
                    //execute locally, send changes to server
                    /*DocumentChanges changes = new DocumentChanges();
                    Collection c = new Collection("tasks", false, transaction.getStorage());
                    Document d = new Document(c, UUID.randomUUID());
                    d.setJsonData("Pridany cez execute a applyChanges 1 ");
                    changes.addDocument(d);
                    remoteStorage.applyChanges(changes, request);*/
                } else {
                    //speculative execution (method called on server)
                    synchronized (unconfirmedRequests){
                        unconfirmedRequests.offer(request);
                        unconfirmedRequests.notify();
                    }
                }


			}
		}
	}

	private final RemoteStorage remoteStorage;
	private final LocalStorage localStorage;

	private final Map<String, TransactionRunnable> speculativeMethods = new HashMap<String, TransactionRunnable>();

	private final Queue<ExecutionRequest> pendingRequests = new LinkedList<ExecutionRequest>();
	private final Queue<ExecutionRequest> unconfirmedRequests = new LinkedList<ExecutionRequest>();

	private RequestHandleThread requestHandleThread;
	private Thread threadForLocalDBRuns;

	private final Object lock = new Object();

	public Storage(StorageSetup setup, RemoteStorage remoteStorage, final LocalStorage localStorage) {
		this.remoteStorage = remoteStorage;
		this.localStorage = localStorage;
		remoteStorage.setListener(new RemoteStorageListener() {

            public void requestCompleted(ExecutionRequest request) {
				// TODO Auto-generated method stub
				//odstranit request z unconfirmed...
				System.out.println("REQUEST COMPLETED --- "+request.getMethodName());
			}

			public void changesReceived(RemoteDocumentChange[] changes) {
				// TODO Auto-generated method stub
				//System.out.println("DATA V STORAGE ---  "+changes[0].getType()+"  "+changes[0].getData());
			}

			public void connectionChanged(Connection connection) {
                System.out.println("CONNECTION CHANGED ---"+connection.getState());
            }

			@Override
			public void unsubscribed(String subscriptionName, Error error) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onError(Error error) {
				// TODO Auto-generated method stub
			}

			public void collectionInvalidated(String collection) {
				localStorage.removeCollection(collection);
			}
		});

		//TODO session state
		remoteStorage.start("");

	}

	void setThreadForLocalDBRuns(Thread threadForLocalDBRuns) {
		this.threadForLocalDBRuns = threadForLocalDBRuns;
	}

	boolean sameAsThreadForLocalDBRuns(Thread thread) {
		return threadForLocalDBRuns == thread;
	}

	/**
	 * Returns document collection. If collection does not exist, it will be
	 * created.
	 * 
	 * @param name
	 * @return
	 */
	public Collection getCollection(String name) {

		//TODO from table or memory?

		//pre testy zatial
		return new Collection(name, false, this);
	}

	public Subscription subscribe(String subscriptionName, Object... arguments) {
		return remoteStorage.subscribe(subscriptionName, arguments);
	}

	public void registerSpeculativeMethod(String name, TransactionRunnable runnable) {
		if (name == null ||  runnable == null){
			//TODO error?
		}
		speculativeMethods.put(name, runnable);
	}

	public ExecutionRequest execute(TransactionRunnable runnable, Object... arguments) {
		ExecutionRequest executionRequest = executeAsync(runnable, arguments);
		executionRequest.await();
		return executionRequest;
	}

	public ExecutionRequest executeAsync(TransactionRunnable runnable, Object... arguments) {
        String seed = "";
        IdGenerator idGenerator = remoteStorage.getIdGenerator(seed);
        Transaction transaction = new Transaction(this, idGenerator);
		ExecutionRequest executionRequest = new ExecutionRequest(transaction);
		executionRequest.setRunnable(runnable);
		executionRequest.setAttributes(arguments);
		executionRequest.setContext(new DefaultExecutionContext(this));

		synchronized (pendingRequests){
			pendingRequests.offer(executionRequest);
			pendingRequests.notify();
		}

		return executionRequest;
	}

    /**
     * Executes given TransactionRunnable only locally; can only use non-synchronized collections.
     * @param runnable user given code inside
     * @param arguments argements
     * @return created ExecutionRequest for this execution
     */
    public ExecutionRequest executeLocally(TransactionRunnable runnable, Object... arguments) {
        ExecutionRequest executionRequest = executeAsync(runnable, arguments);
        executionRequest.await();
        return executionRequest;
	}

	public ExecutionRequest executeLocallyAsync(TransactionRunnable runnable, Object... arguments) {
        String seed = "";
        IdGenerator idGenerator = remoteStorage.getIdGenerator(seed);
        Transaction transaction = new Transaction(this, idGenerator);
        ExecutionRequest executionRequest = new ExecutionRequest(transaction);
        executionRequest.setLocal(true);
        executionRequest.setRunnable(runnable);
        executionRequest.setAttributes(arguments);
        executionRequest.setContext(new DefaultExecutionContext(this));

        synchronized (pendingRequests){
            pendingRequests.offer(executionRequest);
            pendingRequests.notify();
        }

        return executionRequest;
	}

	public ExecutionRequest call(String methodName, Object... arguments) {
		ExecutionRequest executionRequest = callAsync(methodName, arguments);
		executionRequest.await();
		return executionRequest;
	}

	public ExecutionRequest callAsync(String methodName, Object... arguments) {
		// Odosle call request do vlakna, kde sa asynchronne
		// no serialozovane spracovavaju modifikujuce kody.

		// Postup spracovania:
		// ak ma metoda lokalne registrovanu verziu, tak sa v transakcii
		// odsimuluje lokalny kod
		// poziadavka na call aj s transakciou sa zapise do RemoteStorage
		// ked sa call vykona, transakcia sa vyhodi zo zoznamu transakcii (jej
		// zmeny sa ignoruju).


        String seed = "";
        IdGenerator idGenerator = remoteStorage.getIdGenerator(seed);
		Transaction transaction = new Transaction(this, idGenerator);
		ExecutionRequest executionRequest = new ExecutionRequest(transaction);
        executionRequest.setMethodName(methodName);

		if (speculativeMethods.containsKey(methodName)){
			TransactionRunnable methodRunnable = speculativeMethods.get(methodName);
			executionRequest.setRunnable(methodRunnable);
		}

		executionRequest.setAttributes(arguments);
		executionRequest.setContext(new DefaultExecutionContext(this));

		remoteStorage.call(methodName, arguments, "", executionRequest);

		if (executionRequest.getRunnable() == null) {
			synchronized (unconfirmedRequests) {
				unconfirmedRequests.offer(new ExecutionRequest(null));
				unconfirmedRequests.notify();
			}
		} else {
			synchronized (pendingRequests) {
				pendingRequests.offer(new ExecutionRequest(null));
				pendingRequests.notify();
			}
		}

		return executionRequest;
	}

	/**
	 * Starts the self-synchronizing storage.
	 */
	public void start(String sessionState) {
		// nezabudnut poriesit stop a opatovny start
		requestHandleThread = new RequestHandleThread();
        new Thread(requestHandleThread).start();
		remoteStorage.start(sessionState);
	}

	/**
	 * Stops the self-synchronizing storage.
	 */
	public void stop() {
		String state = remoteStorage.getSessionState();
		remoteStorage.stop();
	}
}

/*
*
* cally - vola metodu, z tych co su v mape
* pridaju do pendingRequests - v tom handleri sa to tam aj zavola?
* ze som skoncil ako zistim
*
* */