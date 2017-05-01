package net.jards.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves as setup of concrete collection for LocalStorage. Holds name, indexes and other local database settings.
 */
public class CollectionSetup {

    /**
     * prefix for this user
     */
    private String prefix;
    /**
     * name of collection
     */
    private final String name;
    /**
     * true if collection created from this setup should be local
     */
    private final boolean local;

    /**
     * indexes for this collection (key is name(property name), value is type of index ("text" or "int" only now)
     */
    private final Map<String, String> indexes = new HashMap<>();
    /**
     * indexes set in order (needed to get same hash in local storage start method)
     */
    private final List<String> indexesOrder = new ArrayList<>();

    /**
     * Public constructor
     * @param prefix prefix of this user
     * @param name name of collection created from this setup
     * @param local true if collection should be local, else false
     * @param indexes indexes used in this collection (with text type)
     */
    public CollectionSetup(String prefix, String name, boolean local, String... indexes) {
        this.prefix = prefix;
        this.name = name;
        this.local = local;
        for (String index: indexes){
            this.indexes.put(index, "text");
            this.indexesOrder.add(index);
        }
    }

    /**
     * Method to add index (default type ("text"))
     * @param name name of index/property name from document content
     */
    public void addIndex(String name){
        this.indexes.put(name, "text");
        this.indexesOrder.add(name);
    }

    /**
     * Method to add "int" index
     * @param name name name of index/property name from document content
     */
    public void addIntIndex(String name){
        this.indexes.put(name, "int");
        this.indexesOrder.add(name);
    }

    /**
     * Method to add "text" index.
     * @param name name name of index/property name from document content
     */
    public void addStringIndex(String name){
        this.indexes.put(name, "text");
        this.indexesOrder.add(name);
    }

    /**
     * Check if this collection has specified index.
     * @param name index name to check
     * @return true if collection has this index
     */
    public boolean hasIndex(String name){
        return indexes.containsKey(name);
    }

    /**
     * Method to set prefix.
     * @param prefix prefix that will be used
     */
    protected void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Method to get prefix.
     * @return prefix used
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Method to get indexes map
     * @return map of indexes where key is name and value type of index
     */
    public Map<String, String> getIndexes() {
        return indexes;
    }

    /**
     * Method to get ordered indexes
     * @return list of indexes names
     */
    public List<String> getOrderedIndexes() {
        return indexesOrder;
    }

    /**
     * Method to get full name of collection (prefix+name)
     * @return full name of collection
     */
    public String getFullName() {
        return prefix +name;
    }

    /**
     * @return true if this collection is set to be local
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * @return name of collection
     */
    public String getName() {
        return name;
    }
}
