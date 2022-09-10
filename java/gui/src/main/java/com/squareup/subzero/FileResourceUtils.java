package com.squareup.subzero;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class FileResourceUtils {

  // Get all files from a folder inside the JAR
  public final List<Path> getPathsFromResourcesJAR(String folder)
      throws URISyntaxException, IOException {
    List<Path> filePaths;

    String jarPath = getClass()
        .getProtectionDomain()
        .getCodeSource()
        .getLocation().toURI().getPath();

    URI uri = URI.create("jar:file:" + jarPath);
    try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
      filePaths = Files.list(fs.getPath(folder))
          .filter(Files::isRegularFile)
          .collect(Collectors.toList());
      filePaths.sort(Comparator.naturalOrder());
    }

    return filePaths;
  }

  public final InputStream getFileFromResourceAsStream(String filePath) {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inSteam = classLoader.getResourceAsStream(filePath);

    if (inSteam == null) {
      throw new IllegalArgumentException(filePath + ": file not found.");
    } else {
      return inSteam;
    }
  }
}
