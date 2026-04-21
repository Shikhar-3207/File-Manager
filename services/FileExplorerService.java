package service;

import operations.*;
import threading.*;
import java.nio.file.Path;
import java.util.concurrent.Future;

public class FileExplorerService {

    private static FileExplorerService instance;
    private final ThreadPoolManager threadPool;
    private final UndoManager undoManager;

    private FileExplorerService() {
        this.threadPool = ThreadPoolManager.getInstance();
        this.undoManager = new UndoManager();
    }

    public static synchronized FileExplorerService getInstance() {
        if (instance == null) {
            instance = new FileExplorerService();
        }
        return instance;
    }

    public Future<Void> copy(Path source, Path destination,
                             ProgressPublisher.ProgressListener listener) {
        CopyOperation op = new CopyOperation(source, destination);
        if (listener != null) {
            ProgressPublisher publisher = new ProgressPublisher(op, listener);
            publisher.execute(); // SwingWorker handles its own thread
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

    public void undo() {
        undoManager.undo();
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }
}