package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.RemoteStorageError;

import java.util.*;

import static net.jards.core.ExecutionRequest.RequestType.*;
import static net.jards.core.RemoteDocumentChange.ChangeType.*;

/**
 * Transparent storage implementation that combines data from local and remote
 * storage. The implementation is thread-safe.
 */
public class Storage {

    private class RequestsLocalHandlingThread extends Thread {
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


				ExecutionRequest executionRequest;
				synchronized (pendingRequestsLocal) {
                    while (pendingRequestsLocal.isEmpty()){
                        try {
                            pendingRequestsLocal.wait();
					    } catch (InterruptedException e) {
						    e.printStackTrace();
					    }
                    }
                    executionRequest = pendingRequestsLocal.poll();
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
				if (executionRequest.isExecuteLocally()){
                    //only local execution, just execute (that was done already)
                    DocumentChanges documentChanges = transaction.getLocalChanges();
                    applyChangesOnOpenedCursors(documentChanges);
                    executionRequest.ready();
				} else if (executionRequest.isExecute()){
                    //execute locally, send changes to server and apply them on unconfirmed requests
                    DocumentChanges documentChanges = transaction.getLocalChanges();
                    synchronized (pendingRequestsRemote){
                        pendingRequestsRemote.offer(executionRequest);
                        pendingRequestsRemote.notify();
                    }
                    applyChangesOnOpenedCursors(documentChanges);
                    executionRequest.ready();
                } else if (executionRequest.isCall()){
                    //speculative execution (method called on server), local changes to unconfirmed requests
                    synchronized (unconfirmedRequestsLocal){
                        unconfirmedRequestsLocal.offer(executionRequest);
                        unconfirmedRequestsLocal.notify();
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

    private class RequestsRemoteHandlingThread extends Thread {
        @Override
        public void run() {
            while (running){
                //wait if missing remote connection
                boolean recoveredFromPause = false;
                while (paused){
                    recoveredFromPause = true;
                    synchronized (pendingRequestsRemote){
                        try {
                            pendingRequestsRemote.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //continue - if we want stop work, to check if running is true
                if (recoveredFromPause)
                    continue;

                //TODO read from one queue, do job, put to another

                //remoteStorage.applyChanges(documentChanges, executionRequest);

                //TODO solve connection issues - on connection change change pause (with notify!!!)

            }
        }
    }

	private final RemoteStorage remoteStorage;
	private final LocalStorage localStorage;
    private final StorageSetup storageSetup;

	private final Map<String, TransactionRunnable> speculativeMethods = new HashMap<String, TransactionRunnable>();

    private final Queue<UpdateDbRequest> remoteChanges = new LinkedList<UpdateDbRequest>();
	private final Queue<ExecutionRequest> pendingRequestsLocal = new LinkedList<ExecutionRequest>();
	private final Queue<ExecutionRequest> unconfirmedRequestsLocal = new LinkedList<ExecutionRequest>();

    private final Queue<ExecutionRequest> pendingRequestsRemote = new LinkedList<ExecutionRequest>();
    private final Queue<ExecutionRequest> unconfirmedRequestsRemote = new LinkedList<ExecutionRequest>();

	private RequestsLocalHandlingThread requestsLocalHandlingThread;
	private Thread threadForLocalDBRuns;

    private RequestsRemoteHandlingThread requestsRemoteHandlingThread;

    private final JSONPropertyExtractor jsonPropertyExtractor;

	private final Object lock = new Object();

    private volatile boolean running = false;
    private volatile boolean paused = true;

	public Storage(StorageSetup setup, RemoteStorage remoteStorage, final LocalStorage localStorage) {
		this.remoteStorage = remoteStorage;
		this.localStorage = localStorage;
        this.jsonPropertyExtractor = setup.getJsonPropertyExtractor();
        storageSetup = setup;
		remoteStorage.setListener(new RemoteStorageListener() {

            public void requestCompleted(ExecutionRequest request) {
				//removeDocument request from unconfirmed...
				System.out.println("REQUEST COMPLETED --- "+request.getMethodName());
                boolean inUnconfirmedRequests = true;
                //first check if requests have already been run, if it's still in pending queue, remove it
                synchronized (pendingRequestsLocal){
                    if (pendingRequestsLocal.contains(request)){
                        pendingRequestsLocal.remove(request);
                        inUnconfirmedRequests = false;
                    }
                }
                //if requests isn't in pending queue, it should be in unconfirmed, remove it from there
                if (inUnconfirmedRequests){
                    synchronized (unconfirmedRequestsLocal){
                        if (unconfirmedRequestsLocal.contains(request)){
                            unconfirmedRequestsLocal.remove(request);
                        }
                    }
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
                synchronized (pendingRequestsLocal){
                    pendingRequestsLocal.offer(null);
                    pendingRequestsLocal.notify();
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
        Subscription subscription = null;
        ExecutionRequest executionRequest = new ExecutionRequest(null);
        executionRequest.setSubscriptionObject(subscription);
        executionRequest.setAttributes(arguments);
        remoteStorage.subscribe(subscriptionName, arguments);
        return subscription;
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
        executionRequest.setRequestType(Execute);
		executionRequest.setRunnable(runnable);
		executionRequest.setAttributes(arguments);
		executionRequest.setContext(new DefaultExecutionContext(this));

		synchronized (pendingRequestsLocal){
			pendingRequestsLocal.offer(executionRequest);
			pendingRequestsLocal.notify();
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
        executionRequest.setRequestType(ExecuteLocally);
        executionRequest.setRunnable(runnable);
        executionRequest.setAttributes(arguments);
        executionRequest.setContext(new DefaultExecutionContext(this));

        synchronized (pendingRequestsLocal){
            pendingRequestsLocal.offer(executionRequest);
            pendingRequestsLocal.notify();
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
        executionRequest.setRequestType(Call);
        executionRequest.setMethodName(methodName);

		if (speculativeMethods.containsKey(methodName)){
			TransactionRunnable methodRunnable = speculativeMethods.get(methodName);
			executionRequest.setRunnable(methodRunnable);
		}

		executionRequest.setAttributes(arguments);
		executionRequest.setContext(new DefaultExecutionContext(this));

		remoteStorage.call(methodName, arguments, "", executionRequest);

		if (executionRequest.getRunnable() == null) {
			synchronized (unconfirmedRequestsLocal) {
				unconfirmedRequestsLocal.offer(new ExecutionRequest(null));
				unconfirmedRequestsLocal.notify();
			}
		} else {
			synchronized (pendingRequestsLocal) {
				pendingRequestsLocal.offer(new ExecutionRequest(null));
				pendingRequestsLocal.notify();
			}
		}

		return executionRequest;
	}

	/**
	 * Starts the self-synchronizing storage.
	 */
	public void start(String sessionState) {
        running = true;
        paused = true;
        //run thread for local work
		requestsLocalHandlingThread = new RequestsLocalHandlingThread();
        new Thread(requestsLocalHandlingThread).start();
        //run thread for remote work
        requestsRemoteHandlingThread = new RequestsRemoteHandlingThread();
        new Thread(requestsRemoteHandlingThread).start();
        //start storages
        //TODO read and return queues from database in localStorage start (unconfirmed and pending work)
        localStorage.start();
		remoteStorage.start(sessionState);
	}

	/**
	 * Stops the self-synchronizing storage.
	 */
	public void stop() {
        //wake up threads and stop them
        running = false;
        paused = false;
        synchronized (pendingRequestsLocal){
            pendingRequestsLocal.notify();
        }
        synchronized (pendingRequestsRemote){
            pendingRequestsRemote.notify();
        }
        //save state and queues with work
		String state = remoteStorage.getSessionState();
		remoteStorage.stop();
        //TODO save all queues here
        localStorage.stop(unconfirmedRequestsLocal);
	}
}

/*
*
* cally - vola metodu, z tych co su v mape
* pridaju do pendingRequestsLocal - v tom handleri sa to tam aj zavola?
* ze som skoncil ako zistim
*
* */