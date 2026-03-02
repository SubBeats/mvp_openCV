package ru.screenmon.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class StoragePaths {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Path basePath;

    public StoragePaths(@Value("${app.storage-path:./storage}") String storagePath) {
        this.basePath = Paths.get(storagePath).toAbsolutePath();
    }

    public Path base() {
        return basePath;
    }

    public Path forScreenDate(Long screenId, LocalDate date) {
        return basePath.resolve(String.valueOf(screenId)).resolve(DATE_DIR.format(date));
    }

    public Path forFrame(Long screenId, Instant timestamp, String filename) {
        LocalDate date = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
        return forScreenDate(screenId, date).resolve(filename != null ? filename : timestamp.toEpochMilli() + ".jpg");
    }
}
