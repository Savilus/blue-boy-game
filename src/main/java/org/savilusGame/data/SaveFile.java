package org.savilusGame.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Owns JSON encoding and atomic replacement of a single save file. */
final class SaveFile {
  private final Path path;
  private final ObjectMapper mapper = new ObjectMapper()
      .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);

  SaveFile(Path path) {
    this.path = path.toAbsolutePath();
  }

  boolean exists() {
    return Files.exists(path);
  }

  SaveGame read() throws IOException {
    return mapper.readValue(path.toFile(), SaveGame.class);
  }

  void write(SaveGame save) throws IOException {
    Path parent = path.getParent();
    Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, "save-", ".tmp");
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), save);
      Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
