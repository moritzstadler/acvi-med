import java.io.IOException;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws IOException, SQLException {
        if (args.length != 4) {
            System.out.println("Missing arguments! Format: <filname> <host> <user> <password>");
        }

        String fileName = args[0];
        String host = args[1];
        String user = args[2];
        String password = args[3];

        String tableName = determineTableName(fileName);

        SqlConnector.getInstance().connect(host, user, password);
        SqlConnector.getInstance().dropTable(tableName);

        Importer importer = new Importer();

        try {
            importer.importFile(fileName, tableName, true);
            int lines = importer.importFile(fileName, tableName, false);
            System.out.println("Successfully imported " + lines + " lines!");
        } catch (Exception exception) {
            System.out.println("Sorry, there was an error importing your file: ");
            System.out.println(exception.getLocalizedMessage());
            throw exception;
        }
    }

    /**
     * determines the sql compatible table name from the filename
     * @param fileName the file path including extension
     * @return the clean table name
     */
    private static String determineTableName(String fileName) {
        String tableName = fileName;
        String[] fileNameSplit = fileName.split("\\.");
        if (fileNameSplit.length >= 1) {
            tableName = fileNameSplit[fileNameSplit.length - 2];
        }
        if (tableName.contains("/")) {
            String[] tableNameSplit = tableName.split("/");
            if (tableNameSplit.length > 0) {
                tableName = tableNameSplit[tableNameSplit.length - 1];
            }
        }
        tableName = tableName.replaceAll("-", "_").replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (tableName.matches("^[0-9]+.*")) {
            tableName = "t" + tableName;
        }
        return tableName;
    }

}
