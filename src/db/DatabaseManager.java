package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Responsibilities:
 *   1. Open (or create) the SQLite database at vault/fileexplorer.db
 *   2. Read schema.sql from the classpath and execute it
 *   3. Provide a single shared Connection to all DAOs
 *   4. Close the connection cleanly when the app exits
 * Usage:
 *   Connection conn = DatabaseManager.getInstance().getConnection();
 */
public class DatabaseManager {

    // ------------------------------------------------------------------ config
    private static final String DB_PATH = "vault/fileexplorer.db";
    private static final String DB_URL  = "jdbc:sqlite:" + DB_PATH;

    // schema.sql must be on the classpath — put it in src/db/
    private static final String SCHEMA_FILE = "/db/schema.sql";

    // --------------------------------------------------------------- singleton
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            // Make sure the vault folder exists before SQLite tries to open the file
            new java.io.File("vault").mkdirs();

            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);

            System.out.println("[DB] Connected  : " + DB_URL);

            runSchema();

        } catch (SQLException e) {
            throw new RuntimeException("[DB] Connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the single instance, creating it on first call.
     * synchronized keeps it thread-safe when multiple threads start at once.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ------------------------------------------------------------------ schema

    /**
     * Reads schema.sql from the classpath and executes every statement.
     * Statements are split on ";" so multi-statement files work correctly.
     * CREATE TABLE IF NOT EXISTS means safe to re-run on every startup.
     */
    private void runSchema() {
        String sql = loadSchemaFile();

        // Split on semicolons, trim whitespace, skip blank entries
        String[] statements = sql.split(";");

        try (Statement stmt = connection.createStatement()) {
            for (String s : statements) {
                String trimmed = s.strip();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                }
            }
            System.out.println("[DB] Schema applied from " + SCHEMA_FILE);
        } catch (SQLException e) {
            throw new RuntimeException("[DB] Schema execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Loads schema.sql as a String from the classpath.
     * Strips single-line comments (-- ...) before returning.
     */
    private String loadSchemaFile() {
        InputStream is = getClass().getResourceAsStream(SCHEMA_FILE);

        if (is == null) {
            throw new RuntimeException(
                "[DB] schema.sql not found on classpath at: " + SCHEMA_FILE +
                "\nMake sure src/db/schema.sql is included in your build output."
            );
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {

            return reader.lines()
                    .filter(line -> !line.stripLeading().startsWith("--")) // remove comments
                    .collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new RuntimeException("[DB] Failed to read schema.sql: " + e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------- public

    /**
     * All DAOs call this to get the shared connection.
     * Never close this connection yourself — let DatabaseManager.close() do it.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Call this once when the application exits (e.g. in a shutdown hook
     * or MainFrame's windowClosing listener).
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
        }
    }
}
