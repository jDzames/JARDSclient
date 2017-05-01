package net.jards.core;

/**
 * Interface for ID generator used in simulations. Its purpose is to generate same id as is generated
 * on server, so documents referenced by id will work even after getting documents from server
 * and throwing away overlay with original document.
 */
public interface IdGenerator {

    /**
     * @return id for new created document
     */
    String getId();
}
