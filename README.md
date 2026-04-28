# File Explorer with Hybrid Encryption

A modern, Java Swing-based file explorer featuring hybrid RSA+AES encryption, recent file tracking, and user favorites.

## Features
- **Hybrid Encryption:** Uses RSA-OAEP for key wrapping and AES-256-GCM for secure, high-performance file encryption.
- **Persistent Security:** RSA keys are generated on first run and stored securely in the `vault/` directory.
- **Recent Files:** Automatically tracks the last 5 files you've opened for quick access.
- **Favorites:** Bookmark your most-used folders.
- **Safe Operations:** Implements "Safe Writing" using temporary files to prevent data loss during encryption/decryption.
- **Undo System:** Thread-safe undo support for major file operations (Rename, Move, Delete, Encrypt, Decrypt).

## Prerequisites
- **Java JDK 17** or higher.
- **Maven** (for building).

## Building the Project
To compile and package the project into a runnable JAR:
```bash
mvn clean package
```

## Running the Application
After building, you can run the JAR from the `target` folder:
```bash
java -jar target/file-explorer-1.0-SNAPSHOT.jar
```

## Maintenance & Testing
To run the database and persistence layer tests:
```bash
mvn exec:java -Dexec.mainClass="db.DBTest"
```

## Storage
All application data (database, encryption keys) is stored in the `vault/` directory.
- `vault/fileexplorer.db`: SQLite database for metadata.
- `vault/public.key` & `vault/private.key`: RSA key pair (DO NOT DELETE).
