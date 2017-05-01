package net.jards.core;

/**
 * Class for query parameters. Not finished, only scratch as of now.
 */
public class Query {

    //http://www.javaworld.com/article/2074938/core-java/too-many-parameters-in-java-methods-part-3-builder-pattern.html
    //https://github.com/pushtorefresh/storio/blob/master/docs/StorIOSQLite.md

    private boolean isRawQuery;
    private String collection;
    private Predicate predicate;
    private String rawQuery;

    protected Query(String collection, Predicate predicate){
        isRawQuery = false;
        this.collection = collection;
        this.predicate = predicate;
    }

    protected Query(String rawQuery){
        isRawQuery = true;
        this.rawQuery = rawQuery;
    }

    /**
     * Start building query with this.
     * @return QueryBuilder which can be used to build query by adding arguments in cycle.
     */
    public static QueryBuilder builder(){
        return new QueryBuilder();
    }

    public String getCollection() {
        return collection;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public String getRawQuery() {
        return rawQuery;
    }

    public boolean isRawQuery() {
        return isRawQuery;
    }
}


/*
*
*
* */