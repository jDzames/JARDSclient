package net.jards.core;

import java.util.*;

/**
 * Transparent storage implementation that combines data from local and remote
 * storage. The implementation is thread-safe.
 */
public class Storage {

	private class RequestHandleThread extends Thread {
		@Override
		public void run() {
			while (true) {
				ExecutionRequest request = null;
				synchronized (pendingRequests) {
					try {
						pendingRequests.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					request = pendingRequests.poll();
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

	private final Object lock = new Object();

	public Storage(StorageSetup setup, RemoteStorage remoteStorage, LocalStorage localStorage) {
		this.remoteStorage = remoteStorage;
		this.localStorage = localStorage;
		remoteStorage.setListener(new RemoteStorageListener() {

			public void requestCompleted(ExecutionRequest request) {
				// TODO Auto-generated method stub

			}

			public void changesReceived(RemoteDocumentChange[] changes) {
				// TODO Auto-generated method stub

			}

			public void connectionChanged() {
				// TODO Auto-generated method stub
				
			}

			public void collectionInvalidated(String collection) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	/**
	 * Returns document collection. If collection does not exist, it will be
	 * created.
	 * 
	 * @param name
	 * @return
	 */
	public Collection getCollection(String name) {
		return null;
	}

	public Subscription subscribe(String subscriptionName, Object... arguments) {
		return remoteStorage.subscribe(subscriptionName, arguments);
	}

	public void registerSpeculativeMethod(String name, TransactionRunnable runnable) {

	}

	public ExecutionRequest execute(TransactionRunnable runnable, Object... arguments) {
		return null;
	}

	public ExecutionRequest executeAsync(TransactionRunnable runnable, Object... arguments) {
		return null;
	}

	public ExecutionRequest executeLocally(TransactionRunnable runnable, Object... arguments) {
		return null;
	}

	public ExecutionRequest executeLocallyAsync(TransactionRunnable runnable, Object... arguments) {
		return null;
	}

	public ExecutionRequest call(String name, Object... arguments) {
		ExecutionRequest state = callAsync(name, arguments);
		state.await();
		return state;
	}

	public ExecutionRequest callAsync(String name, Object... arguments) {
		// Odosle call request do vlakna, kde sa asynchronne
		// no serialozovane spracovavaju modifikujuce kody.

		// Postup spracovania:
		// ak ma metoda lokalne registrovanu verziu, tak sa v transakcii
		// odsimuluje lokalny kod
		// poziadavka na call aj s transakciou sa zapise do RemoteStorage
		// ked sa call vykona, transakcia sa vyhodi zo zoznamu transakcii (jej
		// zmeny sa ignoruju).

		synchronized (pendingRequests) {
			pendingRequests.offer(new ExecutionRequest(null));
			pendingRequests.notify();
		}

		return null;
	}

	/**
	 * Starts the self-synchronizing storage.
	 */
	public void start(String sessionState) {
		// nezabudnut poriesit stop a opatovny start
		requestHandleThread = new RequestHandleThread();
		remoteStorage.start(sessionState);
	}

	/**
	 * Stops the self-synchronizing storage.
	 */
	public void stop() {

	}
}
