package ru.screenmon.upload;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.screenmon.storage.StoragePaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class UploadService {

    private final StoragePaths paths;

    public UploadService(StoragePaths paths) {
        this.paths = paths;
    }

    public String save(Long screenId, Instant timestamp, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        LocalDate date = timestamp != null
            ? timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            : LocalDate.now();
        String filename = (timestamp != null ? timestamp.toEpochMilli() : UUID.randomUUID().toString()) + ".jpg";
        java.nio.file.Path dir = paths.forScreenDate(screenId, date);
        Files.createDirectories(dir);
        java.nio.file.Path target = dir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }
        return target.toString();
    }
}
