-- ============================================================
--  File Explorer Database Schema
--  SQLite 3
--  Location : vault/fileexplorer.db
--  Loaded by : DatabaseManager.java on first run
-- ============================================================

-- ------------------------------------------------------------
-- 1. recent_files
--    Tracks every file the user opens.
--    Trimmed automatically to 50 rows (newest kept).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recent_files (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    path      TEXT    NOT NULL,
    opened_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2. favorites
--    User-bookmarked folders/files.
--    path is UNIQUE so no duplicates.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorites (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    path TEXT    NOT NULL UNIQUE,
    name TEXT    NOT NULL
);

-- ------------------------------------------------------------
-- 3. encrypted_files
--    Stores the RSA-wrapped AES key and IV for every
--    encrypted file. The actual file on disk is replaced
--    with ciphertext; this table is the only way to decrypt.
--    wrapped_aes_key : RSA-OAEP encrypted AES-256 key (BLOB)
--    iv              : 12-byte AES-GCM nonce (BLOB)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS encrypted_files (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path       TEXT    NOT NULL UNIQUE,
    wrapped_aes_key BLOB    NOT NULL,
    iv              BLOB    NOT NULL,
    encrypted_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);
