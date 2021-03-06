package net.jards.core;

import rx.Observable;
import rx.subjects.PublishSubject;

import java.util.*;

import static net.jards.core.ResultSet.DocumentChange.ChangeType.*;

/**
 * Self-updating result set of documents.
 */
public class ResultSet {

    /**
     * checked to true if some documents changed in this result set after some operation
     */
    private boolean newResult;

    /**
     * Listener for changes only. (Not finished implementation yet.)
     */
    public interface ChangeListener {
        /**
         * @param change changed documents that can be used
         */
        void resultChanged(DocumentChanges change);
	}

    /**
     * Listener for actual documents list.
     */
    public interface ActualDocumentsListener {
        /**
         * @param actualDocuments list with actual documents representing result of
         */
        void resultChanged(DocumentList actualDocuments);
    }

    /**
     * Class representing one change in documents.
     */
    static class DocumentChange{

        /**
         * Type of change: add, update, remove.
         */
        enum ChangeType{
            ADD,
            UPDATE,
            REMOVE
        }

        /**
         * type of change
         */
        ChangeType type;
        /**
         * changed document
         */
        public Document document;

        /**
         * @param type type of change
         * @param document changed document
         */
        DocumentChange(ChangeType type, Document document){
            this.type = type;
            this.document = document;
        }
    }

    /**
     * predicate filtering this result
     */
    private final Predicate predicate;
    /**
     * collection that created this result
     */
    private final Collection collection;
    /**
     * Options for result. (Not used here yet.)
     */
    private final ResultOptions resultOptions;

    /**
     * source documents (from database, permanent changes)
     */
    private List<Document> sourceDocuments = new ArrayList<>();
    /**
     * final list of documents, result of source documents + overlays
     */
    private List<Document> finalDocuments = new ArrayList<>();

    /**
     * map containing ordered overlays with documents
     */
    private Map<DocumentChanges, DocumentChanges> overlaysWithChanges = new LinkedHashMap<>();
    /**
     * map of last changes, representing one final overlay (result of overlays)
     */
    private Map<String, DocumentChange> lastChanges = new HashMap<>();

    /**
     * true if result set is closed, else false
     */
    private boolean closed = false;

    /**
     * list of listeners listening for changes only
     */
    private final List<ChangeListener> changeListeners = new ArrayList<>();
    /**
     * list of listeners listening for new documents list after some change
     */
    private final List<ActualDocumentsListener> actualDocumentsListeners = new ArrayList<>();
    /**
     * RxObservables waiting for new changes
     */
    private final List<PublishSubject<DocumentList>> rxObservablesListening = new LinkedList<>();

    /**
     * Constructor for result set with needed options, settings.
     * @param predicate predicate filtering this result set
     * @param collection collection of this result
     * @param resultOptions options for this result set
     */
    public ResultSet(Predicate predicate, Collection collection, ResultOptions resultOptions) {
        this.predicate = predicate;
        this.collection = collection;
        this.resultOptions = resultOptions;
    }

    /**
     * Sets source documents of this result.
     * @param documents new list used as source documents
     */
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
     * Looking at source docs and overlays creates final documents and updates listeners and RxObservable
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

    /**
     * @return true if result set is closed, else false
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * @return simple list of documents
     */
    public DocumentList getDocuments() {
		return new DocumentList(finalDocuments);
	}

    /**
     * @return RxObservable that updates each time changes are made to this result set, uses result documents list
     */
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

    /**
     * @return RxObservable that updates each time changes are made to this result set, uses only changes
     */
    public Observable<DocumentChanges> getAsRxChanges() {
		return null;
	}

    /**
     * Adds change listener
     * @param listener listener to listen for changes of this result set
     */
    public void addChangeListener(ChangeListener listener) {
        this.changeListeners.add(listener);
	}

    /**
     * Removes change listener
     * @param listener listener to remove
     */
    public void removeChangeListener(ChangeListener listener) {
        this.changeListeners.remove(listener);
	}

    /**
     * Adds actual documents listener
     * @param listener listener that listens for actual documents list
     */
    public void addActualDocumentsListener(ActualDocumentsListener listener) {
        this.actualDocumentsListeners.add(listener);
        listener.resultChanged(new DocumentList(finalDocuments));
    }

    /**
     * Remove actual documents listener
     * @param listener specified listener
     */
    public void removeActualDocumentsListener(ActualDocumentsListener listener) {
        this.actualDocumentsListeners.remove(listener);
    }

    /**
     * Applies changes to this result set. Actualizes source documents based on given documents.
     * @param documentChanges changes to apply to this result set
     */
    public void applyChanges(DocumentChanges documentChanges){
        //add documents from document changes
        for (Document document: documentChanges.getAddedDocuments()){
            if (matchDocument(document)){
                newResult = true;
                sourceDocuments.add(document);
                //remove document from overlays
                removeFromOverlays(document);
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
                //remove document from overlays
                removeFromOverlays(document);
            }
        }
        //remove documents
        for (Document document : documentChanges.getRemovedDocuments()) {
            if (matchDocument(document)){
                newResult = true;
                sourceDocuments.removeIf(doc -> doc.getId().equals(document.getId()));
                //remove document from overlays
                removeFromOverlays(document);
            }
        }

        //update final documents
        updateFinalDocuments();
    }

    /**
     * Removes this document from last changes and from all overlays.
     * @param document document to remove
     */
    private void removeFromOverlays(Document document){
        for (DocumentChanges overlay:overlaysWithChanges.values()) {
            overlay.removeDocumentFromChanges(document);
        }
        lastChanges.remove(document.getId());
    }

    /**
     * Adds overlay to this result set.
     * @param changes overlay changes
     */
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
                relevantChanges.addRemovedDocument(document);
            }
        }
        //add these changes to overlay
        this.overlaysWithChanges.put(changes, relevantChanges);
        //update lastChanges
        updateOverlayChangesWithOverlay(changes, relevantChanges);
        //update final documents with actual data
        updateFinalDocuments();
    }

    /**
     * Method to remove overlay.
     * @param changes overlay changes to remove
     */
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

    /**
     * Method to update final changes (overlays result) with another overlay.
     * @param changes original overlay changes
     * @param relevantChanges only relevant changes for this result set
     */
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

    /**
     * Method to filter documents with this result set's predicate.
     * @param document document that will be matched
     * @return true if given document matches this result set's predicate, else false
     */
    private boolean matchDocument(Document document){
        if (predicate == null){
            return this.collection.getName().equals(document.getCollection().getName());
        }

        if (!collection.getName().equals(document.getCollection().getName())){
            return false;
        }

        return predicate.match(document);
    }

    /**
     * @return this result set's collection
     */
    public Collection getCollection() {
        return collection;
    }

    /**
     * Resets source documents of this result set. (Can be used when user subscribes with some RemoteStorage implementations.)
     */
    public void invalidateSourceDocuments() {
        this.sourceDocuments = new ArrayList<>();
        newResult = true;
        updateFinalDocuments();
    }

}
