package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * RecentFilesDAO
 *
 * Handles all database operations for the recent_files table.
 *
 * Called by:
 *   FileExplorerService.openFile()  → insert()
 *   SidebarPanel (on load)          → fetchTop10()
 *   "Clear History" button          → clearAll()
 *
 * Table columns:
 *   id        INTEGER  PK AUTOINCREMENT
 *   path      TEXT     full file path
 *   opened_at DATETIME auto-set by SQLite
 */
public class RecentFilesDAO {

    private final Connection conn;

    public RecentFilesDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }


    /**
     * Records that a file was just opened.
     * After inserting, trims the table so it never exceeds 50 rows.
     *
     * @param path  absolute path of the opened file
     */
    public void insert(String path) {
        String sql = "INSERT INTO recent_files (path) VALUES (?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.executeUpdate();
            trimOld();
        } catch (SQLException e) {
            System.err.println("[RecentFilesDAO] insert error: " + e.getMessage());
        }
    }

    /**
     * Returns the 10 most recently opened file paths, newest first.
     * Used by SidebarPanel to populate the "Recent" list.
     *
     * @return list of paths (may be empty, never null)
     */
    public List<String> fetchTop10() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT path FROM recent_files ORDER BY id DESC LIMIT 10";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getString("path"));
            }
        } catch (SQLException e) {
            System.err.println("[RecentFilesDAO] fetchTop10 error: " + e.getMessage());
        }

        return list;
    }

    /**
     * Deletes every row from recent_files.
     * Triggered by a "Clear History" action in the UI.
     */
    public void clearAll() {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM recent_files");
            System.out.println("[RecentFilesDAO] History cleared.");
        } catch (SQLException e) {
            System.err.println("[RecentFilesDAO] clearAll error: " + e.getMessage());
        }
    }

    /**
     * Keeps only the 50 newest rows.
     * Called automatically after every insert — caller never needs to invoke this.
     */
    private void trimOld() {
        String sql = """
                DELETE FROM recent_files
                WHERE id NOT IN (
                    SELECT id FROM recent_files
                    ORDER BY opened_at DESC
                    LIMIT 50
                )
                """;

        try (Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            if (deleted > 0) {
                System.out.println("[RecentFilesDAO] Trimmed " + deleted + " old row(s).");
            }
        } catch (SQLException e) {
            System.err.println("[RecentFilesDAO] trimOld error: " + e.getMessage());
        }
    }
}
