import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws IOException, SQLException {
        if (args.length != 5) {
            System.out.println("Missing arguments! Format: <filname> <database> <host> <user> <password>");
        }

        String fileName = args[0];
        String database = args[1];
        String host = args[2];
        String user = args[3];
        String password = args[4];

        String tableName = fileName;
        if (fileName.contains(".") && !fileName.startsWith(".")) {
            tableName = fileName.split("\\.")[0].replaceAll("[a-zA-Z0-9_]", "");
        }

        SqlConnector.getInstance().connect(host, user, password);
        SqlConnector.getInstance().useDatabase(database);
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

}
