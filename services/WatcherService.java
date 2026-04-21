package service;

import java.io.IOException;
import java.nio.file.*;

public class WatcherService implements Runnable {

    public interface ChangeListener {
        void onChange(String eventType, Path affectedPath);
    }

    private final Path directory;
    private final ChangeListener listener;
    private volatile boolean running = true;
    private WatchService watchService;

    public WatcherService(Path directory, ChangeListener listener) {
        this.directory = directory;
        this.listener = listener;
    }

    public void start() {
        // Submit to the thread pool — never new Thread() directly
       threading.ThreadPoolManager.getInstance().submit((java.util.concurrent.Callable<Void>) () -> {
    run();
    return null;
});
    }

    public void stop() {
        running = false;
        try {
            if (watchService != null) watchService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            while (running) {
                WatchKey key = watchService.take(); // blocks until event
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = directory.resolve(
                            (Path) event.context());
                    listener.onChange(event.kind().name(), changed);
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            if (running) e.printStackTrace();
        }
    }
}