package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.RemoteStorageError;

import java.util.*;

import static net.jards.core.Connection.STATE.*;
import static net.jards.core.ExecutionRequest.RequestType.*;
import static net.jards.core.RemoteDocumentChange.ChangeType.*;
import static net.jards.core.StorageSetup.RemoteLoginType.DemandLogin;

/**
 * Main/core class of system, using local and remote storage to achieve transparent synchronisation and offline mode.
 * User can perform operations through methods of this class.
 */
public class Storage {

    /**
     * Class that executes local storage oriented requests from storage.
     */
    private class RequestsLocalHandlingThread extends Thread {
		@Override
		public void run() {

			//Set this thread, so program can check and don't allow transactions in another.
			setThreadForLocalDBRuns(Thread.currentThread());

			while (running) {
                //remote changes are written first (if there are some)
                synchronized (remoteChanges) {
                    if (!remoteChanges.isEmpty()){
                        while (!remoteChanges.isEmpty()){
                            //add all of document changes that came from server (in order)
                            UpdateDbRequest updateDbRequest = remoteChanges.poll();
                            try {
                                if (updateDbRequest.isInvalidateCollection()){
                                    String collection = updateDbRequest.getCollectionName();
                                    invalidateCollection(collection);
                                    invalidateOpenedResultSets(collection);
                                } else {
                                    DocumentChanges changes = updateDbRequest.getDocumentChanges();
                                    localStorage.applyDocumentChanges(changes);
                                    applyChangesOnUnconfirmedRequests(changes);
                                    applyChangesOnOpenedResultSets(changes);
                                }
                            } catch (LocalStorageException e) {
                                System.out.println("ERROR: " + e.toString());
                            }
                        }
                    }
                }
                //execute pending requests
				ExecutionRequest executionRequest = null;
				synchronized (pendingRequestsLocal) {
                    if (pendingRequestsLocal.isEmpty() /*&& running*/){
                        try {
                            pendingRequestsLocal.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else{
                        executionRequest = pendingRequestsLocal.poll();
                    }
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
                    applyChangesOnUnconfirmedRequests(documentChanges);
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
                    applyChangesOnUnconfirmedRequests(documentChanges);
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

    /**
     * Class that executes remote storage oriented requests from system.
     * This class will possibly be part of RemoteStorage in future. (pros and cons on both sides)
     */
    private class RequestsRemoteHandlingThread extends Thread {
        @Override
        public void run() {
            while (running){
                //wait if missing remote connection
                boolean recoveredFromPause = false;
                while (disconnectedFromRemoteStorage ){
                    recoveredFromPause = true;
                    //synchronized (connectionLock){
                        try {
                            //try to connect
                            remoteStorage.start(session);
                            //connectionLock.wait();
                            Thread.sleep(400);
                            //System.out.println("trying to connect");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    //}
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
                            executeRequest(request);
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

                executeRequest(request);
                synchronized (unconfirmedRequestsRemote){
                    unconfirmedRequestsRemote.offer(request);
                    unconfirmedRequestsRemote.notify();
                }
            }
        }

        private void executeRequest(ExecutionRequest request) {
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

    /**
     * reference for RemoteStorage that is used with this Storage
     */
    private final RemoteStorage remoteStorage;
    /**
     * reference for LocalStorage that is used with this Storage
     */
    private final LocalStorage localStorage;
    /**
     * StorageSetup for this Storage
     */
    private final StorageSetup storageSetup;
    /**
     * Session (from RemoteStorage) in string representation
     */
    private String session = "";

    /**
     * map containing speculative methods (key is name, value method)
     */
    private final Map<String, TransactionRunnable> speculativeMethods = new HashMap<String, TransactionRunnable>();

    /**
     * queue for pending changes from RemoteStorage
     */
    private final Queue<UpdateDbRequest> remoteChanges = new LinkedList<UpdateDbRequest>();
    /**
     * queue for pending requests from user (local storage oriented)
     */
    private final Queue<ExecutionRequest> pendingRequestsLocal = new LinkedList<ExecutionRequest>();
    /**
     * queue for unconfirmed requests (local storage oriented)
     */
    private final Queue<ExecutionRequest> unconfirmedRequestsLocal = new LinkedList<ExecutionRequest>();

    /**
     * queue for pending requests from user (remote storage oriented)
     */
    private final Queue<ExecutionRequest> pendingRequestsRemote = new LinkedList<ExecutionRequest>();
    /**
     * queue for unconfirmed requests (remote storage oriented)
     */
    private final Queue<ExecutionRequest> unconfirmedRequestsRemote = new LinkedList<ExecutionRequest>();

    /**
     * list of opened result sets
     */
    private final List<ResultSet> openedResultSets = new LinkedList<ResultSet>();

    /**
     * class with thread doing local storage oriented work
     */
    private RequestsLocalHandlingThread requestsLocalHandlingThread;
    /**
     * reference for thread doing local work (for transaction check)
     */
    private Thread threadForLocalDBRuns;

    /**
     * class with thread doing remote storage oriented work
     */
    private RequestsRemoteHandlingThread requestsRemoteHandlingThread;

    //private final Object connectionLock = new Object();

    /**
     * boolean to control local and remote classes with threads work; set to false to end their work
     */
    private volatile boolean running = false;
    /**
     * boolean to control connecting to server
     */
    private volatile boolean disconnectedFromRemoteStorage = true;

    /**
     * type of login to server
     */
    private StorageSetup.RemoteLoginType remoteLoginType;

    /**
     * Constructor for Storage class. Sets local and remote storage and StorageSetup.
     * Creates RemoteStorageListener implementation and sets it to used remote storage.
     * @param setup setup for this Storage
     * @param remoteStorage RemoteStorage implementation to communicate with remote server
     * @param localStorage LocalStorage implementation to communicate with local database
     */
    public Storage(StorageSetup setup, RemoteStorage remoteStorage, LocalStorage localStorage) {
		this.remoteStorage = remoteStorage;
		this.localStorage = localStorage;
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
                    document.setContent(change.getData());
                    if (change.getType() == INSERT){
                        documentChanges.addDocument(document);
                    } else if (change.getType() == UPDATE){
                        documentChanges.updateDocument(document);
                    } else if (change.getType() == REMOVE){
                        documentChanges.addRemovedDocument(document);
                    }
                }
                //add changes to updateDocument db request and offer it to queue
                UpdateDbRequest updateDbRequest = new UpdateDbRequest();
                updateDbRequest.setDocumentChanges(documentChanges);
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
                //System.out.println("CONNECTION CHANGED: "+connection.getState());

                if (connection.getState().equals(Connected)){
                    if (remoteLoginType != DemandLogin){
                        disconnectedFromRemoteStorage = false;
                        synchronized (pendingRequestsRemote){
                            pendingRequestsRemote.offer(null);
                            pendingRequestsRemote.notify();
                        }
                    }
                } else if (connection.getState().equals(LoggedIn)){
                    disconnectedFromRemoteStorage = false;
                    synchronized (pendingRequestsRemote){
                        pendingRequestsRemote.offer(null);
                        pendingRequestsRemote.notify();
                    }
                } else if (connection.getState().equals(Closed) || connection.getState().equals(Disconnected)){
                    disconnectedFromRemoteStorage = true;
                    synchronized (pendingRequestsRemote){
                        pendingRequestsRemote.offer(null);
                        pendingRequestsRemote.notify();
                    }
                }
            }

            @Override
            public void unsubscribed(String subscriptionName, int subscriptionId, RemoteStorageError error) {
                synchronized (activeSubscriptions){
                    if (activeSubscriptions.containsKey(subscriptionName)){
                        activeSubscriptions.remove(subscriptionName);
                    }
                }
            }

            @Override
            public void onError(RemoteStorageError error) {
                //System.out.println("ERROR! source: "+error.source()+", message: "+error.message());
            }

			public void collectionInvalidated(String collection) throws LocalStorageException {
                UpdateDbRequest request = new UpdateDbRequest();
                request.setCollectionName(collection);
                request.setInvalidateCollection(true);
                synchronized (remoteChanges){
                    remoteChanges.offer(request);
                    remoteChanges.notify();
                }
                synchronized (pendingRequestsLocal){
                    pendingRequestsLocal.offer(null);
                    pendingRequestsLocal.notify();
                }
			}
		});
    }

    /**
     * Method to invalidate collection (server may use it and send all data again).
     * @param collection name of collection
     * @throws LocalStorageException thrown if problem happens while working with local database
     */
    private void invalidateCollection(String collection) throws LocalStorageException {
        if (collection == null || "".equals(collection)){
            //reset all collections (new session on remote storage)
            localStorage.invalidateRemoteCollections();
        } else {
            //reset this collection
            CollectionSetup collectionSetup = localStorage.getCollectionSetup(collection);
            localStorage.removeCollection(collectionSetup);
            localStorage.addCollection(collectionSetup);
        }
    }

    /**
     * Method to apply list of changes to document requests.
     *
     * Testing use of it (if server doesn't confirm request,  overlays else overwrite documents
     * already received from server).
     * @param documentChanges list of document changes
     */
    private void applyListOfChangesOnUnconfirmedRequests(List<DocumentChanges> documentChanges) {
        synchronized (unconfirmedRequestsLocal){
            for (ExecutionRequest request:unconfirmedRequestsLocal){
                request.getTransaction().getLocalChanges().removeListOfChangesFromChanges(documentChanges);
            }
            unconfirmedRequestsLocal.notify();
        }
    }

    /**
     * Method to apply changes to document requests.
     *
     * Testing use of it (if server doesn't confirm request,  overlays else overwrite documents
     * already received from server).
     * @param documentChanges changes from server to update overlays
     */
    private void applyChangesOnUnconfirmedRequests(DocumentChanges documentChanges) {
        synchronized (unconfirmedRequestsLocal){
            for (ExecutionRequest request:unconfirmedRequestsLocal){
                request.getTransaction().getLocalChanges().removeChangesFromChanges(documentChanges);
            }
            unconfirmedRequestsLocal.notify();
        }
    }

    /**
     * Adds result set that should be updated with changes then.
     * @param resultSet new created result set
     */
    void addOpenedResultSet(ResultSet resultSet) {
        synchronized (openedResultSets){
            this.openedResultSets.add(resultSet);
            openedResultSets.notify();
        }
    }

    /**
     * Method to apply changes to document requests.
     *
     * Testing use of it (if server doesn't confirm request, overlays else overwrite documents
     * already received from server).
     * @param documentChanges list of changes from server
     */
    private void applyListOfChangesOnOpenedResultSets(List<DocumentChanges> documentChanges) {
        synchronized (openedResultSets){
            openedResultSets.removeIf(ResultSet::isClosed);
            for (ResultSet resultSet:openedResultSets){
                documentChanges.forEach(resultSet::applyChanges);
            }
            openedResultSets.notify();
        }

    }

    /**
     * Method to apply changes to opened result sets.
     *
     * Testing use of it (if server doesn't confirm request, overlays else overwrite documents
     * already received from server).
     * @param documentChanges changes from server
     */
    private void applyChangesOnOpenedResultSets(DocumentChanges documentChanges) {
        synchronized (openedResultSets){
            openedResultSets.removeIf(ResultSet::isClosed);
            for (ResultSet resultSet:openedResultSets){
                resultSet.applyChanges(documentChanges);
            }
            openedResultSets.notify();
        }
    }

    /**
     * Invalidate all opened result sets created by specified collection. Can be used when collection
     * is being invalidated and server sends all data (again).
     * @param collection name of invalidated collection
     */
    private void invalidateOpenedResultSets(String collection){
        synchronized (openedResultSets){
            openedResultSets.removeIf(ResultSet::isClosed);
            for (ResultSet resultSet:openedResultSets){
                if (collection==null || collection.length()==0 ||
                        resultSet.getCollection().equals(collection)){
                    resultSet.invalidateSourceDocuments();
                }
            }
            openedResultSets.notify();
        }
    }

    /**
     * Adds overlay to opened result sets. (Also removes all closed result sets.)
     * @param documentChanges overlay with changes that will be added
     */
    private void addOverlayToOpenedResultSets(DocumentChanges documentChanges){
        synchronized (openedResultSets){
            openedResultSets.removeIf(ResultSet::isClosed);
            for (ResultSet resultSet:openedResultSets){
                resultSet.addOverlayWithChanges(documentChanges);
            }
            openedResultSets.notify();
        }
    }

    /**
     * Removes overlay from opened result sets. (Also removes all closed result sets.)
     * @param documentChanges overlay with changes that will be removed
     */
    private void removeOverlayOfOpenedResultSets(DocumentChanges documentChanges){
        synchronized (openedResultSets){
            openedResultSets.removeIf(ResultSet::isClosed);
            for (ResultSet resultSet:openedResultSets){
                resultSet.removeOverlayWithChanges(documentChanges);
            }
            openedResultSets.notify();
        }
    }

    /**
     * Sets given thread to variable to it's possible to test if executed code is in given thread.
     * @param threadForLocalDBRuns thread where local storage requests are being processed
     */
    private void setThreadForLocalDBRuns(Thread threadForLocalDBRuns) {
		this.threadForLocalDBRuns = threadForLocalDBRuns;
	}

    /**
     * Tests if given thread is same as the one for local storage oriented transactions to run.
     * Used by transaction.
     * @param thread thread to test
     * @return true if threads are same
     */
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
     * @throws LocalStorageException exception thrown if problems with database reads/writes happens
     */
    Collection getOrCreateCollection(String collectionName) throws LocalStorageException {
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

    /**
     * @return true if storage is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return LocalStorage implementation used in this Storage
     */
    /*public*/ LocalStorage getLocalStorage() {
        return localStorage;
    }

    /**
     * @return RemoteStorage implementation used in this Storage
     */
    /*public*/ RemoteStorage getRemoteStorage() {
        return remoteStorage;
    }

    /**
     * map of active subscriptions (key is name, value is request used to subscribe)
     */
    private final Map<String, ExecutionRequest> activeSubscriptions = new HashMap<>();

    /**
     * Method used to subscribe to server (through remotes torage).
     * @param subscriptionName name of subscription
     * @param arguments optional arguments
     */
    public void subscribe(String subscriptionName, Object... arguments) {
        ExecutionRequest executionRequest = new ExecutionRequest(null);
        executionRequest.setRequestType(Subscribe);
        executionRequest.setSubscriptionName(subscriptionName);
        executionRequest.setAttributes(arguments);
        synchronized (pendingRequestsRemote){
            pendingRequestsRemote.offer(executionRequest);
            pendingRequestsRemote.notify();
        }
        synchronized (activeSubscriptions){
            activeSubscriptions.put(subscriptionName, executionRequest);
        }
	}

    /**
     * Unsubscribes from active subscription.
     * @param subscriptionName name of subscription that should be unsubscribed
     * @param arguments optional arguments
     */
    public void unsubscribe(String subscriptionName, Object... arguments){
        ExecutionRequest executionRequest;
        synchronized (activeSubscriptions){
            if (!activeSubscriptions.containsKey(subscriptionName)){
                return;
            }
           executionRequest = activeSubscriptions.get(subscriptionName);
        }
        if (executionRequest==null){
            return;
        }
        executionRequest.setRequestType(Unsubscribe);
        synchronized (pendingRequestsRemote){
            pendingRequestsRemote.offer(executionRequest);
            pendingRequestsRemote.notify();
        }
    }

    /**
     * Adds speculative method (simulation) to storage. This method will be used when call method
     * with same name will be used.
     * @param name name for speculation
     * @param runnable method for speculation (TransactionRunnable implementation)
     */
    public void registerSpeculativeMethod(String name, TransactionRunnable runnable) {
		if (name == null ||  runnable == null){
			//TODO error?
		}
		speculativeMethods.put(name, runnable);
	}

    /**
     * Execute type of method. Storage executes it locally with local storage and then sends changes to server.
     * Blocking version.
     * @param runnable operations that will be executed
     * @param arguments optional arguments
     * @return  created ExecutionRequest for this execution
     */
    public ExecutionRequest execute(TransactionRunnable runnable, Object... arguments) {
		ExecutionRequest executionRequest = executeAsync(runnable, arguments);
		executionRequest.await();
		return executionRequest;
	}

    /**
     * Execute type of method. Storage executes it locally with local storage and then sends changes to server.
     * Asynchronous version.
     * @param runnable operations that will be executed
     * @param arguments optional arguments
     * @return created ExecutionRequest for this execution
     */
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
     * Blocking version.
     * @param runnable user given code inside
     * @param arguments optional arguments
     * @return created ExecutionRequest for this execution
     */
    public ExecutionRequest executeLocally(TransactionRunnable runnable, Object... arguments) {
        ExecutionRequest executionRequest = executeAsync(runnable, arguments);
        executionRequest.await();
        return executionRequest;
	}

    /**
     * Executes given TransactionRunnable only locally; can only use non-synchronized collections.
     * Asynchronous version.
     * @param runnable user given code inside
     * @param arguments optional arguments
     * @return created ExecutionRequest for this execution
     */
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

    /**
     * Call remote method on server. If there is speculation for this methodName it will be
     * executed locally as speculation.
     * Blocking version.
     * @param methodName name of method that will be called
     * @param arguments optional arguments
     * @return created ExecutionRequest for this execution
     */
    public ExecutionRequest call(String methodName, Object... arguments) {
		ExecutionRequest executionRequest = callAsync(methodName, arguments);
		executionRequest.await();
		return executionRequest;
	}

    /**
     * Call remote method on server. If there is speculation for this methodName it will be
     * executed locally as speculation.
     * Blocking version.
     * @param methodName name of method that will be called
     * @param arguments optional arguments
     * @return created ExecutionRequest for this execution
     */
    public ExecutionRequest callAsync(String methodName, Object... arguments) {
        String seed = UUID.randomUUID().toString();
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
     * @param sessionState    session which will be sent to remote server to try to continue work
     * @throws LocalStorageException when problem with setting collections happens
     */
	public void start(String sessionState) throws LocalStorageException {
        session = sessionState;
        running = true;
        disconnectedFromRemoteStorage = true;

        //start local storage and run thread for local work (in this order)
        localStorage.start();

        //if no session, invalidate (reset) all remote collections (if they are empty or with data, new session has started)
        /*if(sessionState == null || sessionState.length() == 0){
            localStorage.invalidateRemoteCollections();
        }*/

        requestsLocalHandlingThread = new RequestsLocalHandlingThread();
        requestsLocalHandlingThread.start();
        //TODO read and return queues from database in localStorage start (unconfirmed and pending work)

        //run thread for remote work and start remotes storage
        requestsRemoteHandlingThread = new RequestsRemoteHandlingThread();
        requestsRemoteHandlingThread.start();

        //start with session
		//remoteStorage.start(sessionState);
	}

    /**
     * Starts the self-synchronizing storage.
     * @throws LocalStorageException exception thrown if problems with database reads/writes happens
     */
    public void start() throws LocalStorageException {
        session = null;
        running = true;
        disconnectedFromRemoteStorage = true;

        //start local storage and run thread for local work (in this order)
        localStorage.start();

        requestsLocalHandlingThread = new RequestsLocalHandlingThread();
        requestsLocalHandlingThread.start();
        //TODO read and return queues from database in localStorage start (unconfirmed and pending work)

        //run thread for remote work and start remotes storage
        requestsRemoteHandlingThread = new RequestsRemoteHandlingThread();
        requestsRemoteHandlingThread.start();

    }

	/**
	 * Stops the self-synchronizing storage.
     * @return session string
     */
	public String stop() {
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
		String sessionState = remoteStorage.getSessionState();
		remoteStorage.stop();
        //TODO save all queues here ...
        localStorage.stop(unconfirmedRequestsLocal);
        return sessionState;
	}
}

