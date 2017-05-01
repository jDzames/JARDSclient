package net.jards.local.sqlite;

import net.jards.core.CollectionSetup;
import net.jards.core.LocalStorage;
import net.jards.core.Predicate;
import net.jards.core.Predicate.*;
import net.jards.core.ResultOptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class for SQLiteLocalStorage for work with predicates.
 */
public class SQLiteQueryGenerator implements LocalStorage.PredicateFilter {

    /**
     * setup of collection that we work with
     */
    private final CollectionSetup collectionSetup;

    /**
     * counter for prepared statement parameters
     */
    private int paramIndex = 1;

    /**
     * Constructor with collection setup.
     * @param collectionSetup used collection setup
     */
    SQLiteQueryGenerator(CollectionSetup collectionSetup){
        this.collectionSetup = collectionSetup;
    }

    /**
     * Method used to help with filtering of supported predicates.
     * @param predicate given predicate
     * @return true for supported predicate, else false
     */
    @Override
    public boolean isAcceptable(Predicate predicate) {
        if (predicate == null || collectionSetup == null){
            return false;
        }

        if (predicate instanceof And){
            return ((And) predicate).getSubPredicates().size() >0;
        }

        if (predicate instanceof Predicate.Equals || predicate instanceof EqualProperties
                || predicate instanceof Compare || predicate instanceof CompareProperties
                || predicate instanceof Or){
            for (String property : predicate.getProperties() ) {
                if (!collectionSetup.hasIndex(property)){
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Method that generates predicates and options part of statement.
     * @param dbConnection connection to database
     * @param p filtering predicate
     * @param resultOptions specified options
     * @return PreparedStatement object
     */
    PreparedStatement generateFilterStatement(Connection dbConnection, Predicate p, ResultOptions resultOptions) {
        try{
            StringBuilder sql = new StringBuilder()
                    .append("select * from ")
                    .append(collectionSetup.getFullName());
            //add predicates (where part)
            if (p!=null){
                sql.append(createPredicatesSql(p));
            }
            //add options (order by only now)
            List<String> orderByProperties = resultOptions.getOrderByProperties();
            List<ResultOptions.OrderBy> orderByTypes = resultOptions.getOrderByType();
            if (orderByProperties!=null && orderByProperties.size()>0){

                sql.append(" order by ");
                for (int i = 0; i < orderByProperties.size() - 1; i++) {
                    sql.append(orderByProperties.get(i)).append(" ")
                            .append(orderByTypes.get(i)).append(", ");
                }
                sql.append(orderByProperties.get(orderByProperties.size()-1)).append(" ")
                        .append(orderByTypes.get(orderByTypes.size()-1)).append(", ");
            }
            sql.append(" ;");
            //create statement
            PreparedStatement preparedStatement = dbConnection.prepareStatement(sql.toString());
            //fill with parameters (predicates)
            if (p!=null){
                addPredicatesParamaters(p, preparedStatement);
            }
            return preparedStatement;
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Method that creates predicates part of sql string.
     * @param p given predicate
     * @return StringBuilder with built sql string for predicates
     */
    private StringBuilder createPredicatesSql(Predicate p){
        StringBuilder sqlPart = new StringBuilder();
        sqlPart.append(" where ");
        if (p instanceof Predicate.Equals){
            return sqlPart.append(" ")
                    .append(p.getProperties().iterator().next())
                    .append(" = ? ");
        }
        if (p instanceof EqualProperties){
            Iterator<String> iterator = p.getProperties().iterator();
            return sqlPart.append(" ").append(iterator.next())
                    .append(" = ")
                    .append(iterator.next()).append(" ");
        }
        if (p instanceof Compare){
            String operator = null;
            switch (((Compare)p).getOperator()){
                case SameAs: operator = " = "; break;
                case NotSameAs: operator = " != "; break;
                case Bigger: operator = " > "; break;
                case BiggerOrEquals: operator = " >= "; break;
                case Smaller: operator = " < "; break;
                case SmallerOrEquals: operator = " <= "; break;
            }
            return sqlPart.append(" ")
                    .append(p.getProperties().iterator().next())
                    .append(operator).append("? ");
        }
        if (p instanceof CompareProperties){
            String operator = null;
            switch (((CompareProperties)p).getOperator()){
                case SameAs: operator = " = "; break;
                case NotSameAs: operator = " != "; break;
                case Bigger: operator = " > "; break;
                case BiggerOrEquals: operator = " >= "; break;
                case Smaller: operator = " < "; break;
                case SmallerOrEquals: operator = " <= "; break;
            }
            Iterator<String> iterator = p.getProperties().iterator();
            return sqlPart.append(" ")
                    .append(iterator.next())
                    .append(operator)
                    .append(iterator.next()).append(" ");
        }
        if (p instanceof And){
            List<Predicate> innerPredicates = ((And)p).getSubPredicates();
            for (int i = 0; i < innerPredicates.size()-1; i++) {
                sqlPart.append(" ").append(createPredicatesSql(innerPredicates.get(i)))
                        .append(" and ");
            }
            sqlPart.append(createPredicatesSql(innerPredicates.get(innerPredicates.size()-1)));
            return sqlPart;
        }
        if (p instanceof Or){
            List<Predicate> innerPredicates = ((Or)p).getSubPredicates();
            for (int i = 0; i < innerPredicates.size()-1; i++) {
                sqlPart.append(" ").append(createPredicatesSql(innerPredicates.get(i)))
                        .append(" or ");
            }
            sqlPart.append(createPredicatesSql(innerPredicates.get(innerPredicates.size()-1)));
            return sqlPart;
        }
        return sqlPart;
    }

    /**
     * Recursive function that fills parameters from predicate in statement.
     * @param p used predicate
     * @param statement statement which parameters will be filled
     * @throws SQLException throws exception if there is problem with statement parameters
     */
    void addPredicatesParamaters(Predicate p, PreparedStatement statement) throws SQLException {
        if (p instanceof Predicate.Equals){
            statement.setString(paramIndex, String.valueOf(((Predicate.Equals)p).getValue()));
            paramIndex++;
            return;
        }
        if (p instanceof EqualProperties){
            return;
        }
        if (p instanceof Compare){
            statement.setString(paramIndex, String.valueOf(((Compare)p).getValue()));
            paramIndex++;
            return;
        }
        if (p instanceof CompareProperties){
            return;
        }
        if (p instanceof And){
            List<Predicate> innerPredicates = ((And)p).getSubPredicates();
            for (Predicate innerPredicate:innerPredicates) {
                addPredicatesParamaters(innerPredicate, statement);
            }
            return;
        }
        if (p instanceof Or){
            List<Predicate> innerPredicates = ((Or)p).getSubPredicates();
            for (Predicate innerPredicate:innerPredicates) {
                addPredicatesParamaters(innerPredicate, statement);
            }
        }
    }

}
