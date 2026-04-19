package db;

/**
 * EncryptedFileRecord — Plain Data Object (no logic)
 * Carries the three values stored in the encrypted_files table.
 * HybridEncryptor creates this object and passes it to EncryptedFileDAO.insert().
 * EncryptedFileDAO.findByPath() returns this object to HybridDecryptor.
 * Fields:
 *   filePath      — absolute path of the encrypted file on disk
 *   wrappedAesKey — AES-256 key encrypted with RSA public key (BLOB bytes)
 *   iv            — 12-byte AES-GCM nonce used during encryption (BLOB bytes)
 */
public class EncryptedFileRecord {

    private final String filePath;
    private final byte[] wrappedAesKey;
    private final byte[] iv;

    /**
     * @param filePath       absolute path of the encrypted file
     * @param wrappedAesKey  RSA-OAEP wrapped AES key bytes
     * @param iv             12-byte GCM nonce bytes
     */
    public EncryptedFileRecord(String filePath, byte[] wrappedAesKey, byte[] iv) {
        this.filePath      = filePath;
        this.wrappedAesKey = wrappedAesKey;
        this.iv            = iv;
    }

    public String getFilePath()      { return filePath; }
    public byte[] getWrappedAesKey() { return wrappedAesKey; }
    public byte[] getIv()            { return iv; }

    @Override
    public String toString() {
        return "EncryptedFileRecord{" +
               "filePath='" + filePath + '\'' +
               ", wrappedAesKey.length=" + (wrappedAesKey != null ? wrappedAesKey.length : 0) +
               ", iv.length=" + (iv != null ? iv.length : 0) +
               '}';
    }
}
