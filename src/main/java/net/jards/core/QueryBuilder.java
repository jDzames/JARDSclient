package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.QueryException;

class QueryBuilder {

    private String sql;
    private String collection;
    private String where;

    public QueryBuilder rawQuery(String sql){
        this.sql = sql;
        return this;
    }

    public QueryBuilder rawQueryArgs(String... args){
        for (int i = 0; i < args.length; i++) {
            this.sql.replaceFirst("\\?", args[i]);
        }
        return this;
    }

    public QueryBuilder collection(String collection){
        this.collection = collection;
        return this;
    }

    public QueryBuilder where(String where){
        this.where = where;
        return this;
    }

    public QueryBuilder whereArgs(String... args){
        for (int i = 0; i < args.length; i++) {
            this.where.replaceFirst("\\?", args[i]);
        }
        return this;
    }

    //... select, having, ..?

    public Query build() throws QueryException {
        if (!"".equals(sql)){
            return new Query(sql);
        }
        if (!"".equals(collection)){
            return new Query(collection, where);
        }
        throw  new QueryException(LocalStorageException.QUERY_BUILDING_EXCEPTION,
                "Query, build method",
                "You have to provide at least collection for query to be used.");
    }

}

/*
* ala storio
*
* */