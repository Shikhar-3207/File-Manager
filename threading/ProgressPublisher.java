package threading;

import operations.FileOperation;
import javax.swing.SwingWorker;
import java.util.List;

public class ProgressPublisher extends SwingWorker<Void, Integer> {

    private final FileOperation operation;
    private final ProgressListener listener;

    public interface ProgressListener {
        void onProgress(int percent);
        void onDone();
    }

    public ProgressPublisher(FileOperation operation, ProgressListener listener) {
        this.operation = operation;
        this.listener = listener;
    }

    @Override
    protected Void doInBackground() throws Exception {
        operation.execute();
        // Poll progress while running
        while (!isDone()) {
            publish((int) operation.getProgress());
            Thread.sleep(100);
        }
        publish(100);
        return null;
    }

    @Override
    protected void process(List<Integer> chunks) {
        // This runs on the EDT — safe to update Swing UI here
        int latest = chunks.get(chunks.size() - 1);
        if (listener != null) listener.onProgress(latest);
    }

    @Override
    protected void done() {
        if (listener != null) listener.onDone();
    }
}