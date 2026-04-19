package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * FavoritesDAO
 * Handles all database operations for the favorites table.
 * Called by:
 *   SidebarPanel  → getAll()      (load bookmarks on startup)
 *   FileListPanel → add()         (right-click → "Add to Favorites")
 *   SidebarPanel  → remove()      (remove button next to bookmark)
 *   FileListPanel → isFavorite()  (show filled/empty star icon)
 * Table columns:
 *   id   INTEGER  PK AUTOINCREMENT
 *   path TEXT     UNIQUE — full folder/file path
 *   name TEXT     display label shown in sidebar
 */
public class FavoritesDAO {

    private final Connection conn;

    public FavoritesDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // -------------------------------------------------------------------- add

    /**
     * Bookmarks a path with a display name.
     * INSERT OR IGNORE means calling this twice for the same path is safe —
     * no duplicate, no exception.
     *
     * @param path  absolute path of the folder or file
     * @param name  label to show in the sidebar (e.g. "Projects")
     */
    public void add(String path, String name) {
        String sql = "INSERT OR IGNORE INTO favorites (path, name) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setString(2, name);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("[FavoritesDAO] Added: " + name + " → " + path);
            } else {
                System.out.println("[FavoritesDAO] Already exists (ignored): " + path);
            }
        } catch (SQLException e) {
            System.err.println("[FavoritesDAO] add error: " + e.getMessage());
        }
    }

    /**
     * Removes a bookmark by its path.
     * Safe to call even if the path is not in the table.
     *
     * @param path  exact path that was used when add() was called
     */
    public void remove(String path) {
        String sql = "DELETE FROM favorites WHERE path = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            int rows = ps.executeUpdate();
            System.out.println("[FavoritesDAO] Removed " + rows + " favorite(s) for: " + path);
        } catch (SQLException e) {
            System.err.println("[FavoritesDAO] remove error: " + e.getMessage());
        }
    }

    /**
     * Returns all bookmarks sorted alphabetically by name.
     * Each entry is a String array: [0] = path, [1] = name.
     * Used by SidebarPanel to build the favorites list.
     *
     * @return list of {path, name} pairs (may be empty, never null)
     */
    public List<String[]> getAll() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT path, name FROM favorites ORDER BY name ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("path"),
                        rs.getString("name")
                });
            }
        } catch (SQLException e) {
            System.err.println("[FavoritesDAO] getAll error: " + e.getMessage());
        }

        return list;
    }

    /**
     * Checks whether a path is already bookmarked.
     * Used by the UI to toggle the star icon (filled = favorite).
     *
     * @param path  path to check
     * @return true if the path exists in the favorites table
     */
    public boolean isFavorite(String path) {
        String sql = "SELECT COUNT(*) FROM favorites WHERE path = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            ResultSet rs = ps.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[FavoritesDAO] isFavorite error: " + e.getMessage());
            return false;
        }
    }
}
