package service;

import model.FileItem;
import model.TextFile;
import model.ImageFile;
import model.VideoFile;
import model.FolderItem;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class SearchService {

    public List<FileItem> search(Path root, String keyword,
                                  String extension, long minSize) {
        List<FileItem> results = new ArrayList<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                                                  BasicFileAttributes attrs) {
                    String name = file.getFileName().toString().toLowerCase();
                    boolean matchesKeyword = keyword == null
                            || keyword.isBlank()
                            || name.contains(keyword.toLowerCase());
                    boolean matchesExt = extension == null
                            || extension.isBlank()
                            || name.endsWith(extension.toLowerCase());
                    boolean matchesSize = attrs.size() >= minSize;

                    if (matchesKeyword && matchesExt && matchesSize) {
                        results.add(toFileItem(file, attrs));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file,
                                                        IOException exc) {
                    return FileVisitResult.CONTINUE; // skip inaccessible files
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
    }

    private FileItem toFileItem(Path file, BasicFileAttributes attrs) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".txt") || name.endsWith(".java")
                || name.endsWith(".md") || name.endsWith(".csv")) {
            return new TextFile(file);
        } else if (name.endsWith(".jpg") || name.endsWith(".png")
                || name.endsWith(".jpeg")) {
            return new ImageFile(file);
        } else if (name.endsWith(".mp4") || name.endsWith(".avi")
                || name.endsWith(".mkv")) {
            return new VideoFile(file);
        } else {
            return new FolderItem(file); // fallback
        }
    }
}