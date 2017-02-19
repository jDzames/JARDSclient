package net.jards.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DefaultJSONPropertyExtractor implements JSONPropertyExtractor{

    private Configuration listConfig = Configuration.defaultConfiguration()
            //.addOptions(Option.ALWAYS_RETURN_LIST)
            .addOptions(Option.SUPPRESS_EXCEPTIONS);
    private Configuration stringConfig = Configuration.defaultConfiguration()
            .addOptions(Option.SUPPRESS_EXCEPTIONS);


    /**
     * Extracts given values from json string.
     * @param jsonString json string from which values will be extracted
     * @param propertyPaths specified names of properties which values to extract
     * @return map with property paths and values
     */
    @Override
    public Map<String, Object> extractPropertyValues(String jsonString, List<String> propertyPaths) {
        Map<String, Object> values = new HashMap<>();
        ReadContext ctx = JsonPath.using(listConfig).parse(jsonString);
        for (String propertyPath:propertyPaths) {
            values.put(propertyPath, ctx.read("$"+propertyPath));
        }
        return values;
    }

    /**
     * Extracts single value of given json string.
     * @param jsonString json string from which value will be extracted
     * @param propertyPath specified name of property which value to extract
     * @return value of specified property
     */
    @Override
    public Object extractPropertyValue(String jsonString, String propertyPath) {
        return JsonPath.using(stringConfig).parse(jsonString).read("$"+propertyPath);
    }
}
