package service;

import model.FileItem;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SearchService {

    public List<FileItem> search(Path root, String keyword, boolean recursive, Consumer<Path> onNavigate) {
        List<FileItem> results = new ArrayList<>();
        FileExplorerService service = FileExplorerService.getInstance();

        if (recursive) {
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (matches(file, keyword)) {
                            results.add(service.toFileItem(file, onNavigate));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!dir.equals(root) && matches(dir, keyword)) {
                            results.add(service.toFileItem(dir, onNavigate));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path entry : stream) {
                    if (matches(entry, keyword)) {
                        results.add(service.toFileItem(entry, onNavigate));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Sort results: Directories first, then by name
        results.sort((a, b) -> {
            boolean aDir = Files.isDirectory(a.getPath());
            boolean bDir = Files.isDirectory(b.getPath());
            if (aDir != bDir) return aDir ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        return results;
    }

    private boolean matches(Path path, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        return path.getFileName().toString().toLowerCase().contains(keyword.toLowerCase());
    }
}
