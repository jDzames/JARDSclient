package net.jards.core;

import net.jards.errors.LocalStorageException;
import net.jards.errors.QueryException;

/**
 * Class for building queries with predicates and options in more natural way (StorIO style?).
 * Only scratch as of now, not finished.
 */
class QueryBuilder {

    private String rawQuery;
    private String collection;
    private Predicate predicate;

    protected QueryBuilder(){}

    public QueryBuilder rawQuery(String rawQuery){
        this.rawQuery = rawQuery;
        return this;
    }

    public QueryBuilder rawQueryArgs(String... args) throws QueryException {
        if (rawQuery == null){
            throw new QueryException(LocalStorageException.QUERY_BUILDING_EXCEPTION,
                    "QueryBuilder, rawQuery",
                    "Raw query string must be specified before using raw query args.",
                    null);
        }
        for (int i = 0; i < args.length; i++) {
            this.rawQuery = this.rawQuery.replaceFirst("\\?", args[i]);
        }
        return this;
    }

    public QueryBuilder collection(String collection){
        this.collection = collection;
        return this;
    }

    public QueryBuilder predicate(Predicate predicate){
        this.predicate = predicate;
        return this;
    }


    //... select, having, ..?

    public Query build() throws QueryException {
        if (rawQuery != null && !"".equals(rawQuery)){
            return new Query(rawQuery);
        }
        if (collection!= null && !"".equals(collection)){
            return new Query(collection, predicate);
        }
        throw  new QueryException(LocalStorageException.QUERY_BUILDING_EXCEPTION,
                "Query, build method",
                "You have to provide at least collection for query to be used.", null);
    }

}

/*
* ako StorIO ?
* */