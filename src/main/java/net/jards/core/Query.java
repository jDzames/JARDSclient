package net.jards.core;

public class Query {

    //http://www.javaworld.com/article/2074938/core-java/too-many-parameters-in-java-methods-part-3-builder-pattern.html
    //https://github.com/pushtorefresh/storio/blob/master/docs/StorIOSQLite.md

    private boolean isRawQuery;
    private String collection;
    private String where;
    private String rawSql;

    Query(String collection, String where){
        isRawQuery = false;
        this.collection = collection;
        this.where = where;
    }

    Query(String rawSql){
        isRawQuery = true;
        this.rawSql = rawSql;
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

    public String getWhere() {
        return where;
    }

    public String getRawSql() {
        return rawSql;
    }

    public boolean isRawQuery() {
        return isRawQuery;
    }
}


/*
*
*
* */