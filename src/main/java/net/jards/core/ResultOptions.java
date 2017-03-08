package net.jards.core;

/**
 * Options for query.
 */
public class ResultOptions {

    private int limit;
    private boolean ascOrdered;
    private boolean descordered;

    public int getLimit() {
        return limit;
    }

    public boolean isAscOrdered() {
        return ascOrdered;
    }

    public boolean isDescordered() {
        return descordered;
    }

    public void setAscOrdered(boolean ascOrdered) {
        this.ascOrdered = ascOrdered;
    }

    public void setDescordered(boolean descordered) {
        this.descordered = descordered;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
