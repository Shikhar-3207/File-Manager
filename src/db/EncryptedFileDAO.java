package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * EncryptedFileDAO
 * Handles all database operations for the encrypted_files table.
 * This table is the ONLY place the wrapped AES key and IV are stored.
 * Without a row here, the file on disk CANNOT be decrypted.
 * Called by:
 *   FileExplorerService.encryptFile() → insert()   (after HybridEncryptor runs)
 *   FileExplorerService.decryptFile() → findByPath() + delete()
 *   FileListPanel (icon overlay)      → isEncrypted()
 * Table columns:
 *   id              INTEGER   PK AUTOINCREMENT
 *   file_path       TEXT      UNIQUE — absolute path of the .enc file on disk
 *   wrapped_aes_key BLOB      RSA-OAEP encrypted AES-256 key bytes
 *   iv              BLOB      12-byte AES-GCM nonce bytes
 *   encrypted_at    DATETIME  auto-set by SQLite
 */
public class EncryptedFileDAO {

    private final Connection conn;

    public EncryptedFileDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Saves the encryption metadata for a file.
     *
     * INSERT OR REPLACE means:
     *   - First time encrypting  → inserts a new row
     *   - Re-encrypting same file → replaces the old row (fresh key + IV)
     *
     * IMPORTANT: wrappedAesKey and iv are raw bytes — stored as BLOB.
     * Never convert them to String — byte data will be corrupted.
     *
     * @param filePath       absolute path of the encrypted file on disk
     * @param wrappedAesKey  RSA-OAEP encrypted AES key (byte array)
     * @param iv             12-byte AES-GCM nonce (byte array)
     */
    public void insert(String filePath, byte[] wrappedAesKey, byte[] iv) {
        String sql = """
                INSERT OR REPLACE INTO encrypted_files
                    (file_path, wrapped_aes_key, iv)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);
            ps.setBytes (2, wrappedAesKey);   // BLOB — setBytes, NOT setString
            ps.setBytes (3, iv);              // BLOB — setBytes, NOT setString
            ps.executeUpdate();

            System.out.println("[EncryptedFileDAO] Saved key for: " + filePath);
        } catch (SQLException e) {
            System.err.println("[EncryptedFileDAO] insert error: " + e.getMessage());
        }
    }

    /**
     * Fetches the encryption record for a given file path.
     *
     * Returns Optional.empty() if:
     *   - The file was never encrypted via this app
     *   - The record was already deleted after decryption
     *
     * The decryption flow:
     *   1. Call findByPath(path) → get EncryptedFileRecord
     *   2. Pass record.getWrappedAesKey() and record.getIv() to HybridDecryptor
     *   3. After successful decryption call delete(path)
     *
     * @param filePath  absolute path to look up
     * @return Optional containing the record, or Optional.empty()
     */
    public Optional<EncryptedFileRecord> findByPath(String filePath) {
        String sql = """
                SELECT file_path, wrapped_aes_key, iv
                FROM encrypted_files
                WHERE file_path = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new EncryptedFileRecord(
                            rs.getString("file_path"),
                            rs.getBytes ("wrapped_aes_key"),  // BLOB → byte[]
                            rs.getBytes ("iv")                // BLOB → byte[]
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[EncryptedFileDAO] findByPath error: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Removes the encryption record after a file has been successfully decrypted.
     * Safe to call even if the path does not exist in the table.
     *
     * @param filePath  path whose record should be deleted
     */
    public void delete(String filePath) {
        String sql = "DELETE FROM encrypted_files WHERE file_path = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);
            int rows = ps.executeUpdate();
            System.out.println("[EncryptedFileDAO] Deleted " + rows + " record(s) for: " + filePath);
        } catch (SQLException e) {
            System.err.println("[EncryptedFileDAO] delete error: " + e.getMessage());
        }
    }

    /**
     * Checks whether a file currently has an active encryption record.
     * Used by FileListPanel to show a lock icon overlay on encrypted files.
     *
     * @param filePath  path to check
     * @return true if a record exists for this path
     */
    public boolean isEncrypted(String filePath) {
        String sql = "SELECT COUNT(*) FROM encrypted_files WHERE file_path = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("[EncryptedFileDAO] isEncrypted error: " + e.getMessage());
            return false;
        }
    }
}
