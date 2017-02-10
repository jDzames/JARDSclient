package net.jards.core;

import rx.Observable;

/**
 * Self-updating result set.
 */
public class ResultSet {

	public interface ChangeListener {
		void resultChanged(DocumentChanges change);
	}


	/**
	 * Closes the result sets.
	 */
	public void close() {

	}

	public DocumentList getDocuments() {
		return null;
	}

	public Observable<DocumentList> getAsRxList() {
		return null;
	}

	public Observable<DocumentChanges> getAsRxChanges() {
		return null;
	}

	public void addListener(ChangeListener listener) {

	}

	public void removeListener(ChangeListener listener) {

	}

	public void addDocumentData(String id, String collectionName, String jsonData){
        new Document();
    }

}

/*
*
*
* */