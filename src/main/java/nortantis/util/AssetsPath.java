package nortantis.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getFileSystem;
import static java.nio.file.FileSystems.newFileSystem;

public class AssetsPath {
    private static Path assetsPath;

    static {
        var items = getProperty("java.class.path").split(":");
        assetsPath = Stream.of(items)
                .map(x -> {
                    if (!x.endsWith(".jar")) return Paths.get(x);
                    var uri = URI.create("jar:file:" + new File(x).toURI().getRawPath());
                    try {
                        return newFileSystem(uri, Map.of()).getPath("/");
                    } catch (FileSystemAlreadyExistsException e) {
                        return getFileSystem(uri).getPath("/");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(x -> x.resolve("assets"))
                .filter(Files::exists)
                .findFirst().get();
    }

    public static Path get() {
        return assetsPath;
    }
}
