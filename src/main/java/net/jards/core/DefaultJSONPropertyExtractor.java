package net.jards.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.jards.errors.JsonFormatException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DefaultJSONPropertyExtractor implements JSONPropertyExtractor{


    /**
     * Extracts given values from json string.
     * @param jsonString json string from which values will be extracted
     * @param propertyPaths specified names of properties which values to extract
     * @return map with property paths and values
     */
    @Override
    public Map<String, Object> extractPropertyValues(String jsonString, List<String> propertyPaths) throws JsonFormatException {
        Map<String, Object> values = new HashMap<>();
        if (jsonString == null || jsonString.length()==0){
            return values;
        }
        if (propertyPaths == null || propertyPaths.size()==0){
            return values;
        }

        for (String propertyPath:propertyPaths) {
            values.put(propertyPath, extractPropertyValueFromJson(new JsonOA(jsonString), propertyPath));
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
    public Object extractPropertyValue(String jsonString, String propertyPath) throws JsonFormatException {
        JsonOA jsonOA =new JsonOA(jsonString);
        return extractPropertyValueFromJson(jsonOA, propertyPath);
    }

    public Object extractPropertyValueFromJson(JsonOA json, String propertyPath) throws JsonFormatException {
        if (propertyPath == null )
            return null;
        if (propertyPath.length()== 0)
            return json;
        try {

        } catch (Exception e){
            throw new JsonFormatException("Wrong format of json String or wrong property path. ",e);
        }
        String[] properties = propertyPath.split("\\.");
        for (int i = 0; i < properties.length; i++) {
            String prop = properties[i];
            if (prop.contains("[")){
                String[] arrayProperties = prop.split("\\[");
                for (int j = 0; j < arrayProperties.length; j++) {
                    String arrayElement = arrayProperties[j];
                    JsonElement element = null;
                    if (arrayElement.charAt(arrayElement.length()-1)==']'){
                        int arrayIndex = Integer.parseInt(arrayElement.substring(0, arrayElement.length()-1));
                        json.getFromJsonArray(arrayIndex);
                    } else {
                        json.getFromJsonObject(arrayElement);
                    }
                    if (i == properties.length-1 && j == arrayProperties.length-1){
                        return json.toString();
                    }
                }
            } else {
                if (i == properties.length-1){
                    return json.getFromJsonObject(prop).toString();
                }
                json.getFromJsonObject(prop);
            }
        }
        return json;
    }

    public class JsonOA{

        private JsonElement jsonElement;

        public JsonOA(String json){
            JsonParser parser = new JsonParser();
            jsonElement = parser.parse(json);
        }
        public JsonOA(JsonElement jsonElement){
            this.jsonElement = jsonElement;
        }

        public JsonElement getFromJsonArray(int i){
            this.jsonElement = jsonElement.getAsJsonArray().get(i);
            return jsonElement;
        }

        public JsonElement getFromJsonObject(String property){
            this.jsonElement = jsonElement.getAsJsonObject().get(property);
            return jsonElement;
        }

        public JsonElement getJsonElement() {
            return jsonElement;
        }

        public void setJsonElement(JsonElement jsonElement) {
            this.jsonElement = jsonElement;
        }

        @Override
        public String toString() {
            if (jsonElement==null){
                return "null";
            }
            return jsonElement.toString();
        }
    }

}
