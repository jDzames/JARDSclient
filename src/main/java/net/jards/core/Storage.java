package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.RemoteStorageError;

import java.util.*;

import static net.jards.core.Connection.STATE.*;
import static net.jards.core.ExecutionRequest.RequestType.*;
import static net.jards.core.RemoteDocumentChange.ChangeType.*;
import static net.jards.core.StorageSetup.RemoteLoginType.DemandLogin;

/**
 * Transparent storage implementation that combines data from local and remote
 * storage. The implementation is thread-safe.
 */
public class Storage {

    private class RequestsLocalHandlingThread extends Thread {
		@Override
		public void run() {

			//Set this thread, so program can check and don't allow transactions in another.
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
                    // write all changes
                    try {
                        localStorage.applyDocumentChanges(remoteDocumentChanges);
                    } catch (LocalStorageException e) {
                        System.out.println("ERROR: " + e.toString());
                    }
                    applyListOfChangesOnOpenedResultSets(remoteDocumentChanges);
                }


				ExecutionRequest executionRequest;
				synchronized (pendingRequestsLocal) {
                    while (pendingRequestsLocal.isEmpty() && running){
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
				TransactionRunnable runnable = executionRequest.getRunnable();
                if (runnable != null){
                    //Executing runnable
                    runnable.run(context, transaction, arguments);
                }

                //Update with changes
				if (executionRequest.isExecuteLocally()){
                    //only local execution, just execute (that was done already)
                    DocumentChanges documentChanges = transaction.getLocalChanges();
                    applyChangesOnOpenedResultSets(documentChanges);
                    executionRequest.ready();
				} else if (executionRequest.isExecute()){
                    //execute locally, send changes to server and apply them on unconfirmed requests
                    DocumentChanges documentChanges = transaction.getLocalChanges();
                    applyChangesOnOpenedResultSets(documentChanges);
                    synchronized (pendingRequestsRemote){
                        pendingRequestsRemote.offer(executionRequest);
                        pendingRequestsRemote.notify();
                    }
                    applyChangesOnOpenedResultSets(documentChanges);
                    executionRequest.ready();
                } else if (executionRequest.isCall()){
                    //speculative execution (method called on server), local changes to unconfirmed requests
                    DocumentChanges documentChanges = transaction.getLocalChanges();
                    addOverlayToOpenedResultSets(documentChanges);
                    synchronized (unconfirmedRequestsLocal){
                        unconfirmedRequestsLocal.offer(executionRequest);
                        unconfirmedRequestsLocal.notify();
                    }
                }
			}
		}
    }

    private class RequestsRemoteHandlingThread extends Thread {
        @Override
        public void run() {
            while (running){
                //wait if missing remote connection
                boolean recoveredFromPause = false;
                while (disconnectedFromRemoteStorage ){
                    recoveredFromPause = true;
                    synchronized (connectionLock){
                        try {
                            //try to connect
                            remoteStorage.start(session);
                            connectionLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //continue - if we want stop work, to check if running is true
                if (recoveredFromPause){
                    if (!running){
                        break;
                    }
                    //do requests from unconfirmed queue again  ????????????
                    synchronized (unconfirmedRequestsRemote){
                        LinkedList<ExecutionRequest> requests = new LinkedList<>();
                        while (!unconfirmedRequestsRemote.isEmpty() && running) {
                            ExecutionRequest request = unconfirmedRequestsRemote.poll();
                            callRequest(request);
                            requests.add(request);
                        }
                        unconfirmedRequestsRemote.addAll(requests);
                        unconfirmedRequestsRemote.notify();
                    }
                }

                //read from one queue, do job, put to another
                ExecutionRequest request;
                synchronized (pendingRequestsRemote){
                    while(running && pendingRequestsRemote.isEmpty()){
                        //wait for some requests
                        try {
                            pendingRequestsRemote.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    request = pendingRequestsRemote.poll();
                }

                if (request == null){
                    continue;
                }

                callRequest(request);
                synchronized (unconfirmedRequestsRemote){
                    unconfirmedRequestsRemote.offer(request);
                    unconfirmedRequestsRemote.notify();
                }
            }
        }

        private void callRequest(ExecutionRequest request) {
            if (request.isCall()){
                remoteStorage.call(request.getMethodName(), request.getAttributes(), request.getSeed(), request);
            } else if (request.isSubscribe()){
                int id = remoteStorage.subscribe(request.getSubscriptionName(), request);
                request.setRemoteCallsId(id);
            } else if (request.isUnsubscribe()) {
                remoteStorage.unsubscribe(request);
            } else if (request.isExecute()){
                remoteStorage.applyChanges(request.getTransaction().getLocalChanges(), request);
            }
        }
    }

	private final RemoteStorage remoteStorage;
	private final LocalStorage localStorage;
    private final StorageSetup storageSetup;
    private String session = "";

	private final Map<String, TransactionRunnable> speculativeMethods = new HashMap<String, TransactionRunnable>();

    private final Queue<UpdateDbRequest> remoteChanges = new LinkedList<UpdateDbRequest>();
	private final Queue<ExecutionRequest> pendingRequestsLocal = new LinkedList<ExecutionRequest>();
	private final Queue<ExecutionRequest> unconfirmedRequestsLocal = new LinkedList<ExecutionRequest>();

    private final Queue<ExecutionRequest> pendingRequestsRemote = new LinkedList<ExecutionRequest>();
    private final Queue<ExecutionRequest> unconfirmedRequestsRemote = new LinkedList<ExecutionRequest>();

    private final Queue<ResultSet> openedResultSets = new LinkedList<ResultSet>();

	private RequestsLocalHandlingThread requestsLocalHandlingThread;
	private Thread threadForLocalDBRuns;

    private RequestsRemoteHandlingThread requestsRemoteHandlingThread;

    private final JSONPropertyExtractor jsonPropertyExtractor;

	private final Object connectionLock = new Object();

    private volatile boolean running = false;
    private volatile boolean disconnectedFromRemoteStorage = true;

    private StorageSetup.RemoteLoginType remoteLoginType;

	public Storage(StorageSetup setup, RemoteStorage remoteStorage, final LocalStorage localStorage) {
		this.remoteStorage = remoteStorage;
		this.localStorage = localStorage;
        this.jsonPropertyExtractor = setup.getJsonPropertyExtractor();
        storageSetup = setup;
        this.remoteLoginType = storageSetup.getRemoteLoginType();
		remoteStorage.setListener(new RemoteStorageListener() {

            public void requestCompleted(ExecutionRequest request) {
                System.out.println("REQUEST COMPLETED --- "+request.getMethodName());

				if (request.isCall()){
                    //remove request from pending/unconfirmed...

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
                        //remove overlay from opened result sets now
                        removeOverlayOfOpenedResultSets(request.getTransaction().getLocalChanges());
                    }
                    synchronized (unconfirmedRequestsRemote){
                        if (unconfirmedRequestsRemote.contains(request)){
                            unconfirmedRequestsRemote.remove(request);
                        }
                    }
                    //if synchronous call, then continue now
                    request.ready();
                } else {
                    //execute, subscribe, unsubscribe
                    //remove from unconfirmed requests (it is only in remote, local part was done (if there was any) )
                    synchronized (unconfirmedRequestsRemote) {
                        if (unconfirmedRequestsRemote.contains(request)) {
                            unconfirmedRequestsRemote.remove(request);
                        }
                    }
                }
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

                if (connection.getState().equals(Connected)){
                    if (remoteLoginType != DemandLogin){
                        disconnectedFromRemoteStorage = false;
                        synchronized (connectionLock){
                            connectionLock.notify();
                        }
                    }
                } else if (connection.getState().equals(LoggedIn)){
                    disconnectedFromRemoteStorage = false;
                    synchronized (connectionLock){
                        connectionLock.notify();
                    }
                } else if (connection.getState().equals(Closed) || connection.getState().equals(Disconnected)){
                    disconnectedFromRemoteStorage = true;
                    synchronized (connectionLock){
                        connectionLock.notify();
                    }
                }
            }

            @Override
            public void unsubscribed(String subscriptionName, int subscriptionId, RemoteStorageError error) {
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

    void addOpenedResultSet(ResultSet resultSet) {
        synchronized (openedResultSets){
            this.openedResultSets.offer(resultSet);
            openedResultSets.notify();
        }
    }

    private void applyListOfChangesOnOpenedResultSets(List<DocumentChanges> documentChanges) {
        openedResultSets.removeIf(ResultSet::isClosed);
        for (ResultSet resultSet:openedResultSets){
            documentChanges.forEach(resultSet::applyChanges);
        }
    }

    private void applyChangesOnOpenedResultSets(DocumentChanges documentChanges) {
        openedResultSets.removeIf(ResultSet::isClosed);
        for (ResultSet resultSet:openedResultSets){
            resultSet.applyChanges(documentChanges);
        }
    }

    private void addOverlayToOpenedResultSets(DocumentChanges documentChanges){
        openedResultSets.removeIf(ResultSet::isClosed);
        for (ResultSet resultSet:openedResultSets){
            resultSet.addOverlayWithChanges(documentChanges);
        }
    }

    private void removeOverlayOfOpenedResultSets(DocumentChanges documentChanges){
        openedResultSets.removeIf(ResultSet::isClosed);
        for (ResultSet resultSet:openedResultSets){
            resultSet.removeOverlayWithChanges(documentChanges);
        }
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

    public ExecutionRequest subscribe(String subscriptionName, Object... arguments) {
        ExecutionRequest executionRequest = new ExecutionRequest(null);
        executionRequest.setRequestType(Subscribe);
        executionRequest.setSubscriptionName(subscriptionName);
        executionRequest.setAttributes(arguments);
        synchronized (pendingRequestsRemote){
            pendingRequestsRemote.offer(executionRequest);
            pendingRequestsRemote.notify();
        }
        return executionRequest;
	}

    /**
     * Unsubscribes from active subscription.
     * @param executionRequest execution request you want to unsubscribe, the one returned by by subscribe method
     */
    public void unsubscribe(ExecutionRequest executionRequest){
        executionRequest.setRequestType(Unsubscribe);
        synchronized (pendingRequestsRemote){
            pendingRequestsRemote.offer(executionRequest);
            pendingRequestsRemote.notify();
        }
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
        executionRequest.setSeed(seed);

		if (speculativeMethods.containsKey(methodName)){
			TransactionRunnable methodRunnable = speculativeMethods.get(methodName);
			executionRequest.setRunnable(methodRunnable);
		}

		executionRequest.setAttributes(arguments);
		executionRequest.setContext(new DefaultExecutionContext(this));

		synchronized (pendingRequestsRemote){
            pendingRequestsRemote.offer(executionRequest);
            pendingRequestsRemote.notify();
        }

		if (executionRequest.getRunnable() == null) {
			synchronized (unconfirmedRequestsLocal) {
				unconfirmedRequestsLocal.offer(executionRequest);
				unconfirmedRequestsLocal.notify();
			}
		} else {
			synchronized (pendingRequestsLocal) {
				pendingRequestsLocal.offer(executionRequest);
				pendingRequestsLocal.notify();
			}
		}

		return executionRequest;
	}

	/**
	 * Starts the self-synchronizing storage.
	 */
	public void start(String sessionState) throws LocalStorageException {
        session = sessionState;
        running = true;
        disconnectedFromRemoteStorage = true;

        //start local storage and run thread for local work (this order)
        localStorage.start();
		requestsLocalHandlingThread = new RequestsLocalHandlingThread();
        new Thread(requestsLocalHandlingThread).start();
        //TODO read and return queues from database in localStorage start (unconfirmed and pending work)

        //run thread for remote work and start remotes storage
        requestsRemoteHandlingThread = new RequestsRemoteHandlingThread();
        new Thread(requestsRemoteHandlingThread).start();
		remoteStorage.start(sessionState);
	}

	/**
	 * Stops the self-synchronizing storage.
	 */
	public void stop() {
        //wake up threads and stop them
        running = false;
        disconnectedFromRemoteStorage = false;
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