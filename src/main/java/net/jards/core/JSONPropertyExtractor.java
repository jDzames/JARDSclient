package net.jards.core;

import net.jards.errors.JsonFormatException;

import java.util.List;
import java.util.Map;

/**
 * Serves to extract values from json strings. Custom implementations can be used through StorageSetup.
 */
public interface JSONPropertyExtractor {

    /**
     * Extracts given values from json string.
     * @param jsonString json string from which values will be extracted
     * @param propertyPaths specified names of properties which values to extract
     * @return map with property paths and values
     */
    Map<String, Object> extractPropertyValues(String jsonString, List<String> propertyPaths) throws JsonFormatException;

    /**
     * Extracts single value of given json string.
     * @param jsonString json string from which you want to extract value
     * @param propertyPath specified name of property which value to extract
     * @return value of specified property
     */
    Object extractPropertyValue(String jsonString, String propertyPath) throws JsonFormatException;

}
