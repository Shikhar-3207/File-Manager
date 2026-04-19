package db;

import java.util.List;
import java.util.Optional;

/**
 * DBTest — Run this to verify your entire db package works.
 * Expected output:
 *   [DB] Connected  : jdbc:sqlite:vault/fileexplorer.db
 *   [DB] Schema applied from /db/schema.sql
 *   ... (DAO logs)
 *   ALL TESTS PASSED
 */
public class DBTest {

    // Simple pass/fail counter
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        System.out.println("================================================");
        System.out.println("  File Explorer — DB Package Test");
        System.out.println("================================================\n");

        testDatabaseManager();
        testRecentFilesDAO();
        testFavoritesDAO();
        testEncryptedFileDAO();

        System.out.println("\n================================================");
        System.out.printf ("  Results: %d passed, %d failed%n", passed, failed);
        System.out.println("================================================");

        if (failed == 0) {
            System.out.println("  ALL TESTS PASSED — DB package is complete!");
        } else {
            System.out.println("  Some tests FAILED — check errors above.");
        }

        DatabaseManager.getInstance().close();
    }

    private static void testDatabaseManager() {
        section("DatabaseManager");

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            check("getInstance() returns non-null", db != null);
            check("getConnection() is open",
                    db.getConnection() != null && !db.getConnection().isClosed());

            // Calling getInstance() again must return same object (singleton)
            DatabaseManager db2 = DatabaseManager.getInstance();
            check("Singleton — same instance every call", db == db2);

        } catch (Exception e) {
            fail("DatabaseManager threw: " + e.getMessage());
        }
    }

    private static void testRecentFilesDAO() {
        section("RecentFilesDAO");

        RecentFilesDAO dao = new RecentFilesDAO();

        // Start clean
        dao.clearAll();
        List<String> empty = dao.fetchTop10();
        check("After clearAll, fetchTop10 is empty", empty.isEmpty());

        // Insert 3 files
        dao.insert("/home/user/docs/resume.pdf");
        dao.insert("/home/user/pics/photo.jpg");
        dao.insert("/home/user/notes/todo.txt");

        List<String> top10 = dao.fetchTop10();
        check("fetchTop10 returns 3 entries", top10.size() == 3);

        // Newest should be first (todo.txt was inserted last)
        check("Newest file is first", top10.get(0).contains("todo.txt"));

        // Insert 50 more → total 53, trimOld should keep only 50
        for (int i = 0; i < 50; i++) {
            dao.insert("/tmp/file" + i + ".txt");
        }
        // fetchTop10 only returns 10 even if 50 exist
        List<String> afterBulk = dao.fetchTop10();
        check("fetchTop10 returns max 10 after 53 inserts", afterBulk.size() == 10);

        dao.clearAll();
        check("clearAll empties the table", dao.fetchTop10().isEmpty());
    }

    private static void testFavoritesDAO() {
        section("FavoritesDAO");

        FavoritesDAO dao = new FavoritesDAO();

        // Clean slate — remove any leftover test data
        dao.remove("/home/user/docs");
        dao.remove("/home/user/pics");
        dao.remove("/home/user/music");

        // Add two favorites
        dao.add("/home/user/docs",  "Documents");
        dao.add("/home/user/pics",  "Pictures");

        List<String[]> all = dao.getAll();
        check("getAll returns 2 favorites", all.size() == 2);

        // Sorted by name — "Documents" before "Pictures"
        check("Sorted alphabetically by name", all.get(0)[1].equals("Documents"));

        // isFavorite
        check("isFavorite true for added path",    dao.isFavorite("/home/user/docs"));
        check("isFavorite false for unknown path",  !dao.isFavorite("/home/user/music"));

        // Duplicate add — should be ignored (INSERT OR IGNORE)
        dao.add("/home/user/docs", "Documents");
        check("Duplicate add does not create extra row", dao.getAll().size() == 2);

        // Remove
        dao.remove("/home/user/pics");
        check("After remove, size is 1",            dao.getAll().size() == 1);
        check("isFavorite false after remove",       !dao.isFavorite("/home/user/pics"));

        // Cleanup
        dao.remove("/home/user/docs");
    }

    private static void testEncryptedFileDAO() {
        section("EncryptedFileDAO");

        EncryptedFileDAO dao = new EncryptedFileDAO();

        String path = "/home/user/secret.txt";

        // Clean start
        dao.delete(path);
        check("isEncrypted false before insert", !dao.isEncrypted(path));

        // Simulate what HybridEncryptor produces
        // Real usage: wrappedAesKey = 256 bytes (RSA-2048 output), iv = 12 bytes
        byte[] fakeWrappedKey = new byte[256];
        byte[] fakeIV         = new byte[12];
        for (int i = 0; i < 256; i++) fakeWrappedKey[i] = (byte) (i % 128);
        for (int i = 0; i < 12;  i++) fakeIV[i]         = (byte) (i + 1);

        // Insert
        dao.insert(path, fakeWrappedKey, fakeIV);
        check("isEncrypted true after insert", dao.isEncrypted(path));

        // findByPath — must return correct bytes
        Optional<EncryptedFileRecord> opt = dao.findByPath(path);
        check("findByPath returns non-empty Optional", opt.isPresent());

        EncryptedFileRecord record = opt.get();
        check("Record has correct file path",        record.getFilePath().equals(path));
        check("Wrapped key length is 256 bytes",     record.getWrappedAesKey().length == 256);
        check("IV length is 12 bytes",               record.getIv().length == 12);

        // Verify BLOB bytes survived the round-trip intact
        check("Wrapped key bytes are intact",        record.getWrappedAesKey()[0] == (byte) 0
                                                     && record.getWrappedAesKey()[1] == (byte) 1);
        check("IV bytes are intact",                 record.getIv()[0] == (byte) 1
                                                     && record.getIv()[11] == (byte) 12);

        // INSERT OR REPLACE — re-inserting same path replaces the record
        byte[] newKey = new byte[256];
        newKey[0] = (byte) 99;
        dao.insert(path, newKey, fakeIV);
        Optional<EncryptedFileRecord> updated = dao.findByPath(path);
        check("Re-insert replaces old record",       updated.isPresent());
        check("New key byte[0] is 99 after replace", updated.get().getWrappedAesKey()[0] == (byte) 99);

        // findByPath on non-existent path
        Optional<EncryptedFileRecord> missing = dao.findByPath("/does/not/exist.txt");
        check("findByPath returns empty for unknown path", missing.isEmpty());

        // Delete
        dao.delete(path);
        check("isEncrypted false after delete",      !dao.isEncrypted(path));
        check("findByPath empty after delete",        dao.findByPath(path).isEmpty());
    }

    private static void section(String name) {
        System.out.println("\n--- " + name + " ---");
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            passed++;
        } else {
            System.out.println("  [FAIL] " + description);
            failed++;
        }
    }

    private static void fail(String message) {
        System.out.println("  [FAIL] " + message);
        failed++;
    }
}
