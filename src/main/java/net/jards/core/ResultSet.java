package net.jards.core;

import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.*;

import static net.jards.core.ResultSet.DocumentChange.ChangeType.*;

/**
 * Self-updating result set.
 */
public class ResultSet {

    private boolean newResult;

    public interface ChangeListener {
		void resultChanged(DocumentChanges change);
	}

    public interface ActualDocumentsListener {
        void resultChanged(DocumentList actualDocuments);
    }

    static class DocumentChange{

        enum ChangeType{
            ADD,
            UPDATE,
            REMOVE
        }

        ChangeType type;
        public Document document;

        DocumentChange(ChangeType type, Document document){
            this.type = type;
            this.document = document;
        }
    }

    private final Predicate predicate;
    private final Collection collection;
    private final ResultOptions resultOptions;

    private List<Document> sourceDocuments = new ArrayList<>();
    private List<Document> finalDocuments = new ArrayList<>();

    private Map<DocumentChanges, DocumentChanges> overlaysWithChanges = new LinkedHashMap<>();
    private Map<String, DocumentChange> lastChanges = new HashMap<>();

    private boolean closed = false;

    private final List<ChangeListener> changeListeners = new ArrayList<>();
    private final List<ActualDocumentsListener> actualDocumentsListeners = new ArrayList<>();
    private final List<PublishSubject<DocumentList>> rxObservablesListening = new LinkedList<>();

    public ResultSet(Predicate predicate, Collection collection, ResultOptions resultOptions) {
        this.predicate = predicate;
        this.collection = collection;
        this.resultOptions = resultOptions;
    }

    void setResult(List<Document> documents){
        this.sourceDocuments = new ArrayList<>();
        for (Document document:documents) {
            if (matchDocument(document)){
                this.sourceDocuments.add(document);
            }
        }
        newResult = true;
        updateFinalDocuments();
    }

    /**
     * Looking at source docs and overlays creates final docs
     */
    private void updateFinalDocuments(){
        //create newFinalDocuments (newFD)
        List<Document> newFinalDocuments = new ArrayList<>();
        //loop through source docs, check if lastChanges contains doc:
        //   if contains - do not add to newFD
        //   if not contains - add to newFD
        for (Document document: sourceDocuments){
            if (!lastChanges.containsKey(document.getId())){
                newResult = true;
                newFinalDocuments.add(document);
            }
        }
        //loop through lastChanges, if added or changed doc, add it to newFD
        for (DocumentChange documentChange : lastChanges.values()){
            if (documentChange.type != REMOVE){
                newFinalDocuments.add(documentChange.document);
            }
        }
        //TODO order by ?
        //set newFD to new result
        finalDocuments = newFinalDocuments;
        //apply changes to listeners, if something changed
        if (newResult){
            for (ActualDocumentsListener listener:this.actualDocumentsListeners) {
                if (listener!= null){
                    listener.resultChanged(new DocumentList(finalDocuments));
                }
            }
            for (PublishSubject<DocumentList> observable : this.rxObservablesListening){
                try {
                    observable.onNext(getDocuments());
                } catch (Exception e){
                    observable.onError(e);
                }
            }
            newResult = false;
        }
    }

    /**
	 * Closes the result sets.
	 */
	public void close() {
        for (PublishSubject<DocumentList> observable : this.rxObservablesListening){
            try {
                observable.onCompleted();
            } catch (Exception e){
                observable.onError(e);
            }
        }
        sourceDocuments = null;
        finalDocuments = null;
        rxObservablesListening.clear();
        changeListeners.clear();
        actualDocumentsListeners.clear();
        newResult = false;
        closed = true;
	}

    public boolean isClosed() {
        return closed;
    }

    public DocumentList getDocuments() {
		return new DocumentList(finalDocuments);
	}

	public Observable<DocumentList> getAsRxList() {
        PublishSubject<DocumentList> subject = PublishSubject.create();
        rxObservablesListening.add(subject);
        try {
            subject.onNext(this.getDocuments());
        }catch (Exception e){
            subject.onError(e);
        }
		return subject;
	}

	public Observable<DocumentChanges> getAsRxChanges() {
		return null;
	}

	public void addChangeListener(ChangeListener listener) {
        this.changeListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
        this.changeListeners.remove(listener);
	}

    public void addActualDocumentsListener(ActualDocumentsListener listener) {
        this.actualDocumentsListeners.add(listener);
    }

    public void removeActualDocumentsListener(ActualDocumentsListener listener) {
        this.actualDocumentsListeners.remove(listener);
    }

	public void applyChanges(DocumentChanges documentChanges){
        //add documents from document changes
        for (Document document: documentChanges.getAddedDocuments()){
            if (matchDocument(document)){
                newResult = true;
                sourceDocuments.add(document);
            }
        }
        //update documents..
        for (Document document : documentChanges.getUpdatedDocuments()) {
            if (matchDocument(document)){
                for (int i = 0; i < this.sourceDocuments.size(); i++) {
                    if (sourceDocuments.get(i).getId().equals(document.getId())){
                        newResult = true;
                        sourceDocuments.set(i, document);
                    }
                }
            }
        }
        //remove documents
        for (Document document : documentChanges.getRemovedDocuments()) {
            if (matchDocument(document)){
                newResult = true;
                sourceDocuments.removeIf(doc -> doc.getId().equals(document.getId()));
            }
        }
        //update final documents
        updateFinalDocuments();
    }

    void addOverlayWithChanges(DocumentChanges changes){
        //create relevant changes for this result set
        DocumentChanges relevantChanges = new DocumentChanges();
        for (Document document:changes.getAddedDocuments()){
            if (matchDocument(document)){
                relevantChanges.addDocument(document);
            }
        }
        for (Document document:changes.getUpdatedDocuments()){
            if (matchDocument(document)){
                relevantChanges.updateDocument(document);
            }
        }
        for (Document document:changes.getRemovedDocuments()){
            if (matchDocument(document)){
                relevantChanges.removeDocument(document);
            }
        }
        //add these changes to overlay
        this.overlaysWithChanges.put(changes, relevantChanges);
        //update lastChanges
        updateOverlayChangesWithOverlay(changes, relevantChanges);
        //update final documents with actual data
        updateFinalDocuments();
    }

    void removeOverlayWithChanges(DocumentChanges changes){
        //remove this change
        overlaysWithChanges.remove(changes);
        //recreate lastChanges
        lastChanges = new LinkedHashMap<>();
        for(Map.Entry<DocumentChanges, DocumentChanges> entry : overlaysWithChanges.entrySet()){
            updateOverlayChangesWithOverlay(entry.getKey(), entry.getValue());
        }
        //update final documents with actual data
        updateFinalDocuments();
    }

    private void updateOverlayChangesWithOverlay(DocumentChanges changes, DocumentChanges relevantChanges){
        //add all relevant added documents that are not used yet
        for (Document document : relevantChanges.getAddedDocuments()){
            newResult = true;
            lastChanges.put(document.getId(), new DocumentChange(ADD, document));
        }
        //update - all changes for this collection (remove those), relevant changes (update those)
        for (Document document : changes.getUpdatedDocuments()){
            if (this.collection.getName().equals(document.getCollection().getName())){
                newResult = true;
                lastChanges.put(document.getId(), new DocumentChange(REMOVE, document));
            }
        }
        for (Document document : relevantChanges.getUpdatedDocuments()){
            newResult = true;
            lastChanges.put(document.getId(), new DocumentChange(UPDATE, document));
        }
        //remove
        for (Document document : relevantChanges.getRemovedDocuments()){
            newResult = true;
            lastChanges.put(document.getId(), new DocumentChange(REMOVE, document));
        }
    }

    private boolean matchDocument(Document document){
        if (predicate == null){
            return this.collection.getName().equals(document.getCollection().getName());
        }

        if (!collection.getName().equals(document.getCollection().getName())){
            return false;
        }

        return predicate.match(document);
    }

}

/*
*
*
* */