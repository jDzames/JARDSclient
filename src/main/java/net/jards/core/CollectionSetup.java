package net.jards.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves as version of collection for LocalStorage. Holds also indexes and other local database stuff.
 */
public class CollectionSetup {

    private final String tablePrefix;
    private final String name;
    private final boolean local;

    //Map and list of indexes. Key is index name (column name) and value is type of index. def, int, string. List is to have order for
    private final Map<String, String> indexes = new HashMap<>();
    private final List<String> indexesOrder = new ArrayList<>();

    public CollectionSetup(String tablePrefix, String name, boolean local, String... indexes) {
        this.tablePrefix = tablePrefix;
        this.name = name;
        this.local = local;
        for (String index: indexes){
            this.indexes.put(index, "default");
            this.indexesOrder.add(index);
        }
    }

    public void addIndex(String name){
        this.indexes.put(name, "text");
        this.indexesOrder.add(name);
    }

    public void addIntIndex(String name){
        this.indexes.put(name, "int");
        this.indexesOrder.add(name);
    }

    public void addStringIndex(String name){
        this.indexes.put(name, "text");
        this.indexesOrder.add(name);
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public Map<String, String> getIndexes() {
        return indexes;
    }

    public List<String> getOrderedIndexes() {
        return indexesOrder;
    }

    public String getFullName() {
        return tablePrefix+name;
    }

    public boolean isLocal() {
        return local;
    }

    public String getName() {
        return name;
    }
}
