package net.jards.core;

import rx.Observable;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-updating result set.
 */
public class ResultSet {

    public interface ChangeListener {
		void resultChanged(DocumentChanges change);
	}

    private final List<Document> originalQueryDocuments;
    private List<Document> actualQueryDocuments;

    private boolean closed = false;

    private final List<ChangeListener> changeListeners = new ArrayList<>();

    public ResultSet(List<Document> originalQueryDocuments) {
        this.originalQueryDocuments = originalQueryDocuments;
    }

    /**
	 * Closes the result sets.
	 */
	public void close() {
        closed = true;
	}

    public boolean isClosed() {
        return closed;
    }

    public DocumentList getDocuments() {
		return new DocumentList(actualQueryDocuments);
	}

	public Observable<DocumentList> getAsRxList() {
		return null;
	}

	public Observable<DocumentChanges> getAsRxChanges() {
		return null;
	}

	public void addListener(ChangeListener listener) {
        this.changeListeners.add(listener);
	}

	public void removeListener(ChangeListener listener) {
        this.changeListeners.remove(listener);
	}

	public void applyChanges(DocumentChanges documentChanges){
        //add documents from document changes
        this.actualQueryDocuments.addAll(documentChanges.getAddedDocuments());
        //update documents..
        for (Document document : documentChanges.getUpdatedDocuments()) {
            for (int i = 0; i < this.actualQueryDocuments.size(); i++) {

            }
        }
        //remove documents

        for (ChangeListener listener:this.changeListeners) {
            listener.resultChanged(documentChanges);
        }
    }

}

/*
*
*
* */