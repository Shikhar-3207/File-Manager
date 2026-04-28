package service;

import model.*;
import operations.*;
import threading.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class FileExplorerService {

    private static FileExplorerService instance;
    private final ThreadPoolManager threadPool;
    private final UndoManager undoManager;
    private final db.EncryptedFileDAO encryptedFileDAO;
    private final db.FavoritesDAO favoritesDAO;
    private final db.RecentFilesDAO recentFilesDAO;

    private FileExplorerService() {
        this.threadPool = ThreadPoolManager.getInstance();
        this.undoManager = new UndoManager();
        this.encryptedFileDAO = new db.EncryptedFileDAO();
        this.favoritesDAO = new db.FavoritesDAO();
        this.recentFilesDAO = new db.RecentFilesDAO();
    }

    public static synchronized FileExplorerService getInstance() {
        if (instance == null) {
            instance = new FileExplorerService();
        }
        return instance;
    }

    public List<FileItem> listContents(Path path, Consumer<Path> onNavigate) {
        List<FileItem> items = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                items.add(toFileItem(entry, onNavigate));
            }
        } catch (IOException e) {
            System.err.println("Error listing contents: " + e.getMessage());
        }
        return items;
    }

    public FileItem toFileItem(Path path, Consumer<Path> onNavigate) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            String name = path.getFileName().toString().toLowerCase();
            long size = attrs.size();
            java.time.LocalDateTime lastModified = java.time.LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(), java.time.ZoneId.systemDefault());

            if (attrs.isDirectory()) {
                return new FolderItem(path, size, lastModified, onNavigate);
            }

            if (encryptedFileDAO.isEncrypted(path.toString())) {
                return new EncryptedFile(path, size, lastModified);
            }

            if (name.endsWith(".txt") || name.endsWith(".java") || name.endsWith(".md") || name.endsWith(".csv")) {
                return new TextFile(path, size, lastModified);
            } else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) {
                return new ImageFile(path, size, lastModified);
            } else if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv")) {
                return new VideoFile(path, size, lastModified);
            } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg")) {
                return new AudioFile(path, size, lastModified);
            } else {
                return new GenericFile(path, size, lastModified);
            }
        } catch (IOException e) {
            return new GenericFile(path, 0, java.time.LocalDateTime.now());
        }
    }

    public Future<Void> copy(Path source, Path destination,
                             ProgressPublisher.ProgressListener listener) {
        CopyOperation op = new CopyOperation(source, destination);
        if (listener != null) {
            ProgressPublisher publisher = new ProgressPublisher(op, listener);
            publisher.execute();
            return null;
        }
        return threadPool.submit(new FileTask(op));
    }

    public Future<Void> move(Path source, Path destination) {
        MoveOperation op = new MoveOperation(source, destination);
        undoManager.push(op);
        return threadPool.submit(new FileTask(op));
    }

    public Future<Void> delete(Path target) {
        DeleteOperation op = new DeleteOperation(target);
        undoManager.push(op);
        return threadPool.submit(new FileTask(op));
    }

    public Future<Void> zip(Path source, Path zipTarget) {
        ZipOperation op = new ZipOperation(source, zipTarget);
        return threadPool.submit(new FileTask(op));
    }

    public Future<Void> rename(Path source, Path target) {
        RenameOperation op = new RenameOperation(source, target);
        undoManager.push(op);
        return threadPool.submit(new FileTask(op));
    }

    public Future<Void> encrypt(Path file) {
        EncryptOperation op = new EncryptOperation(file);
        return threadPool.submit(new FileTask(op));
    }

    public Future<Void> decrypt(Path file) {
        DecryptOperation op = new DecryptOperation(file);
        return threadPool.submit(new FileTask(op));
    }

    public void undo() {
        undoManager.undo();
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public void addFavorite(Path path) {
        favoritesDAO.add(path.toString(), path.getFileName().toString());
    }

    public void removeFavorite(Path path) {
        favoritesDAO.remove(path.toString());
    }

    public List<String[]> getFavorites() {
        return favoritesDAO.getAll();
    }

    public boolean isFavorite(Path path) {
        return favoritesDAO.isFavorite(path.toString());
    }

    public void trackRecent(Path path) {
        recentFilesDAO.insert(path.toString());
    }

    public List<String> getRecentFiles() {
        return recentFilesDAO.fetchTop10();
    }
}
