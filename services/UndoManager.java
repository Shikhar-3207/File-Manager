package service;

import operations.FileOperation;
import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {

    private static final int MAX_HISTORY = 20;
    private final Deque<FileOperation> history = new ArrayDeque<>();

    public void push(FileOperation op) {
        if (history.size() >= MAX_HISTORY) {
            history.removeLast(); // drop oldest if full
        }
        history.push(op);
    }

    public void undo() {
        if (!history.isEmpty()) {
            FileOperation op = history.pop();
            try {
                op.undo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    public int size() {
        return history.size();
    }
}