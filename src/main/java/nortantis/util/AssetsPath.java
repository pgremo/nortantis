package nortantis.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getFileSystem;
import static java.nio.file.FileSystems.newFileSystem;

public class AssetsPath {
    private static final Path assetsPath;

    static {
        try {
            var items = getProperty("java.class.path").split(":");
            assetsPath = Stream.of(items)
                    .map(x -> {
                        Path path = Paths.get(x);
                        if (!x.endsWith(".jar")) return path;
                        var uri = URI.create("jar:file:" + path.toUri().getRawPath());
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
                    .findFirst().orElseThrow(() -> new NoSuchFileException("assets"));
        } catch (NoSuchFileException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path get() {
        return assetsPath;
    }

    public static Path get(String first, String... rest){
        return get().resolve(get().getFileSystem().getPath(first, rest));
    }
}
