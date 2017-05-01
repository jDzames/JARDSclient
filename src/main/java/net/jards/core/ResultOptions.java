package net.jards.core;

import java.util.LinkedList;
import java.util.List;

/**
 * Class with options for query.
 */
public class ResultOptions {

    /**
     * Enum for order - asc and desc.
     */
    public enum OrderBy{
        ASC,
        DESC
    }

    /**
     * limit for number of documents in result
     */
    private int limit;

    /**
     * properties used to order result
     */
    private List<String> orderByProperties = new LinkedList<>();
    /**
     * types of order for corresponding properties
     */
    private List<OrderBy> orderByType = new LinkedList<>();

    /**
     * @return limit of documents in result
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit set limit of result
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Add option to order documents by property (ORDER BY ... in sql)
     * @param property property to use to order result
     * @param type type of order (asc, desc)
     */
    public void addOrderByOption(String property, OrderBy type){
        this.orderByProperties.add(property);
        this.orderByType.add(type);
    }

    /**
     * @return list of types of order
     */
    public List<OrderBy> getOrderByType() {
        return orderByType;
    }

    /**
     * @return list of properties used in order
     */
    public List<String> getOrderByProperties() {
        return orderByProperties;
    }
}
