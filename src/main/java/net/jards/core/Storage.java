package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.RemoteStorageError;

import java.util.*;

import static net.jards.core.RemoteDocumentChange.ChangeType.*;

/**
 * Transparent storage implementation that combines data from local and remote
 * storage. The implementation is thread-safe.
 */
public class Storage {

    private class RequestHandleThread extends Thread {
		@Override
		public void run() {

			//Set this thread, so program can check and dont allow transactions in another.
			setThreadForLocalDBRuns(Thread.currentThread());

			while (running) {

                List<DocumentChanges> remoteDocumentChanges = new LinkedList<>();
                synchronized (remoteChanges) {
                    if (!remoteChanges.isEmpty()){
                        while (!remoteChanges.isEmpty()){
                            //add all of document changes that came from server (in order)
                            UpdateDbRequest updateDbRequest = remoteChanges.poll();
                            remoteDocumentChanges.add(updateDbRequest.getDocumentChanges());
                        }
                    }
                }
                if (!remoteDocumentChanges.isEmpty()){
                    //TODO write all changes
                    try {
                        localStorage.applyDocumentChanges(remoteDocumentChanges);
                    } catch (LocalStorageException e) {
                        System.out.println("ERROR: " + e.toString());
                    }
                    applyListOfChangesOnOpenedCursors(remoteDocumentChanges);
                }


				ExecutionRequest executionRequest = null;
				synchronized (pendingRequests) {
                    while (pendingRequests.isEmpty()){
                        try {
                            pendingRequests.wait();
					    } catch (InterruptedException e) {
						    e.printStackTrace();
					    }
                    }
                    executionRequest = pendingRequests.poll();
				}
				if (executionRequest == null){
					continue;
				}
				ExecutionContext context = executionRequest.getContext();
				Transaction transaction = executionRequest.getTransaction();
				Object[] arguments = executionRequest.getAttributes();
                String methodName = executionRequest.getMethodName();
				TransactionRunnable runnable = executionRequest.getRunnable();
                if (runnable != null){
                    //Executing runnable
                    runnable.run(context, transaction, arguments);
                }

                //Update with changes
				if (executionRequest.isLocal()){
                    //only local execution, just execute (that was done already)
                    DocumentChanges documentChanges = transaction.getLocalChanges();
                    applyChangesOnOpenedCursors(documentChanges);
                    executionRequest.ready();
				} else if (methodName == null || methodName == ""){
                    //execute locally, send changes to server and apply them on unconfirmed requests
                    DocumentChanges documentChanges = transaction.getLocalChanges();
                    remoteStorage.applyChanges(documentChanges, executionRequest);
                    applyChangesOnOpenedCursors(documentChanges);
                    executionRequest.ready();
                } else {
                    //speculative execution (method called on server), local changes to unconfirmed requests
                    synchronized (unconfirmedRequests){
                        unconfirmedRequests.offer(executionRequest);
                        unconfirmedRequests.notify();
                    }
                }


			}
		}

        private void applyListOfChangesOnOpenedCursors(List<DocumentChanges> documentChanges) {
            //TODO upgrades documents in opened cursors.. how? compare, id,..?
            // execute does it twice (local execution, changes from server (meteor))
        }

        private void applyChangesOnOpenedCursors(DocumentChanges documentChanges) {
            //TODO upgrades documents in opened cursors.. how? compare, id,..?
            // execute does it twice (local execution, changes from server (meteor))
        }
    }

	private final RemoteStorage remoteStorage;
	private final LocalStorage localStorage;
    private final StorageSetup storageSetup;

	private final Map<String, TransactionRunnable> speculativeMethods = new HashMap<String, TransactionRunnable>();

    private final Queue<UpdateDbRequest> remoteChanges = new LinkedList<UpdateDbRequest>();
	private final Queue<ExecutionRequest> pendingRequests = new LinkedList<ExecutionRequest>();
	private final Queue<ExecutionRequest> unconfirmedRequests = new LinkedList<ExecutionRequest>();

	private RequestHandleThread requestHandleThread;
	private Thread threadForLocalDBRuns;

    private final JSONPropertyExtractor jsonPropertyExtractor;

	private final Object lock = new Object();

    private volatile boolean running = false;

	public Storage(StorageSetup setup, RemoteStorage remoteStorage, final LocalStorage localStorage) {
		this.remoteStorage = remoteStorage;
		this.localStorage = localStorage;
        this.jsonPropertyExtractor = setup.getJsonPropertyExtractor();
        storageSetup = setup;
		remoteStorage.setListener(new RemoteStorageListener() {

            public void requestCompleted(ExecutionRequest request) {
				//removeDocument request from unconfirmed...
				System.out.println("REQUEST COMPLETED --- "+request.getMethodName());
                synchronized (unconfirmedRequests){
                    unconfirmedRequests.remove(request);
                }
                //if synchronous call, then continue now
                request.ready();

			}

			public void changesReceived(RemoteDocumentChange[] changes) {
				//Convert remote changes into documents
                DocumentChanges documentChanges = new DocumentChanges();
                for (RemoteDocumentChange change:changes){
                    Document document = null;
                    try {
                        //if this document's collection doesn't exist, create it
                        document = new Document(getOrCreateCollection(change.getCollection()), change.getId());
                    } catch (LocalStorageException e) {
                        e.printStackTrace();
                        continue;
                    }
                    document.setJsonData(change.getData());
                    if (change.getType() == INSERT){
                        documentChanges.addDocument(document);
                    } else if (change.getType() == UPDATE){
                        documentChanges.updateDocument(document);
                    } else if (change.getType() == REMOVE){
                        documentChanges.removeDocument(document);
                    }
                }
                //add changes to updateDocument db request and offer it to queue
                UpdateDbRequest updateDbRequest = new UpdateDbRequest(documentChanges);
                synchronized (remoteChanges){
                    remoteChanges.offer(updateDbRequest);
                    remoteChanges.notify();
                }
                synchronized (pendingRequests){
                    pendingRequests.offer(null);
                    pendingRequests.notify();
                }
			}

			public void connectionChanged(Connection connection) {
                System.out.println("CONNECTION CHANGED: "+connection.getState());
            }

            @Override
            public void unsubscribed(String subscriptionName, RemoteStorageError error) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onError(RemoteStorageError error) {
                System.out.println("ERROR! source: "+error.source()+", message: "+error.message());
            }

			public void collectionInvalidated(String collection) throws LocalStorageException {
				localStorage.removeCollection(collection);
			}
		});

		//TODO session state
		remoteStorage.start("");
    }

	private void setThreadForLocalDBRuns(Thread threadForLocalDBRuns) {
		this.threadForLocalDBRuns = threadForLocalDBRuns;
	}

	boolean sameAsThreadForLocalDBRuns(Thread thread) {
		return threadForLocalDBRuns == thread;
	}

	/**
	 * Returns document collection. If collection have not been specified in StorageSetup, returns null.
	 * 
	 * @param collectionName name of collection
	 * @return selected collection or null if collection was not found
	 */
	public Collection getCollection(String collectionName) {
        CollectionSetup collectionSetup = localStorage.getCollectionSetup(collectionName);
        if (collectionSetup == null){
            return null;
        }
		return new Collection(collectionSetup, this);
	}

    /**
     * Returns document collection. If collection have not been specified in StorageSetup,
     * creates new (not local) collection and returns it.
     *
     * @param collectionName name of collection
     * @return selected collection or new created (not local) collection
     */
    public Collection getOrCreateCollection(String collectionName) throws LocalStorageException {
        CollectionSetup collectionSetup = localStorage.getCollectionSetup(collectionName);
        if (collectionSetup == null){
            //collectionSetup = new CollectionSetup(localStorage.getPrefix(), collectionName, false);
            //localStorage.addCollectionSetup(collectionSetup);
            //localStorage.removeCollection(collectionSetup);
            //localStorage.addCollection(collectionSetup);
            return new Collection(collectionName, false, this);
        }
        return new Collection(collectionSetup, this);
    }

    public boolean isRunning() {
        return running;
    }

    /*public*/ LocalStorage getLocalStorage() {
        return localStorage;
    }

    /*public*/ RemoteStorage getRemoteStorage() {
        return remoteStorage;
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
        transaction.setLocal(true);
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
        transaction.setSpeculation(true);
		ExecutionRequest executionRequest = new ExecutionRequest(transaction);
        executionRequest.setSpeculation(true);
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
        running = true;
		requestHandleThread = new RequestHandleThread();
        new Thread(requestHandleThread).start();
        localStorage.start();
		remoteStorage.start(sessionState);
	}

	/**
	 * Stops the self-synchronizing storage.
	 */
	public void stop() {
        running = false;
		String state = remoteStorage.getSessionState();
		remoteStorage.stop();
        localStorage.stop(unconfirmedRequests);
	}
}

/*
*
* cally - vola metodu, z tych co su v mape
* pridaju do pendingRequests - v tom handleri sa to tam aj zavola?
* ze som skoncil ako zistim
*
* */