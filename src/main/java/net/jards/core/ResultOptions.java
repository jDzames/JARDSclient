package net.jards.core;

import java.util.LinkedList;
import java.util.List;

/**
 * Options for query.
 */
public class ResultOptions {

    public enum OrderBy{
        ASC,
        DESC
    }

    private int limit;

    private List<String> orderByProperties = new LinkedList<>();
    private List<OrderBy> orderByType = new LinkedList<>();

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void addOrderByOption(String property, OrderBy type){
        this.orderByProperties.add(property);
        this.orderByType.add(type);
    }

    public List<OrderBy> getOrderByType() {
        return orderByType;
    }

    public List<String> getOrderByProperties() {
        return orderByProperties;
    }
}
