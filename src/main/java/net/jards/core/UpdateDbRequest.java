package net.jards.core;

/**
 * Created by jDzama on 21.2.2017.
 */
public class UpdateDbRequest {

    private DocumentChanges documentChanges;

    public UpdateDbRequest() {
    }

    public UpdateDbRequest(DocumentChanges documentChanges) {
        this.documentChanges = documentChanges;
    }

    public DocumentChanges getDocumentChanges() {
        return documentChanges;
    }

    public void setDocumentChanges(DocumentChanges documentChanges) {
        this.documentChanges = documentChanges;
    }
}
