package net.jards.local.sqlite;

/**
 * Created by jDzama on 21.1.2017.
 */
public class SQLiteTest {

    public static void main(String[] args) {
        /*Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }*/
        /*String jsonTest = "{\"menu\": {" +
                "  \"id\": \"file\"," +
                "  \"value\": \"File\"," +
                "  \"popup\": {" +
                "    \"menuitem\": [" +
                "      {\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"}," +
                "      {\"value\": \"Open\", \"onclick\": \"OpenDoc()\"}," +
                "      {\"value\": \"Close\", \"onclick\": \"CloseDoc()\"}" +
                "    ]" +
                "  }" +
                "}}";
        JSONPropertyExtractor jsonPropertyExtractor = new DefaultJSONPropertyExtractor();
        String value = (String)jsonPropertyExtractor.extractPropertyValue(jsonTest, "menu.popup.menuitem[0].value");
        System.out.println(value);*/
    }

}
