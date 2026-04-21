package threading;

import operations.FileOperation;
import java.util.concurrent.Callable;

public class FileTask implements Callable<Void> {

    private final FileOperation operation;

    public FileTask(FileOperation operation) {
        this.operation = operation;
    }

    @Override
    public Void call() throws Exception {
        operation.execute();
        return null;
    }
}
