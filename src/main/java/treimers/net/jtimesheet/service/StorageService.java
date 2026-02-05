package treimers.net.jtimesheet.service;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

import treimers.net.jtimesheet.storage.StorageData;

public class StorageService {
    private final ObjectMapper objectMapper;

    public StorageService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public StorageData load(Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), StorageData.class);
    }

    public void save(Path path, StorageData data) throws IOException {
        objectMapper.writeValue(path.toFile(), data);
    }
}
