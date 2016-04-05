package netdava.jbakery.web.watch;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

@Slf4j
@Data
public abstract class AbstractWatchDir {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;

    private boolean trace = false;

    /**
     * Creates a WatchService and registers the given directory
     */
    AbstractWatchDir(Path dir, boolean recursive) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.recursive = recursive;

        if (recursive) {
            log.debug("Scanning {} ...\n", dir);
            registerAll(dir);
            log.debug("Done.");
        } else {
            register(dir);
        }

        // enable trace after initial registration
        this.trace = true;
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                log.debug("register: {}\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    log.debug("update: {} -> {}\n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        for (; ; ) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                log.error("WatchKey not recognized!!");
                continue;
            }
            List<WatchEvent<Path>> filteredEvents = key.pollEvents().stream()
                    .filter(watchEvent -> watchEvent.kind() != OVERFLOW)
                    .map(watchEvent -> {
                        // Context for directory entry event is the file name of entry
                        WatchEvent<Path> ev = cast(watchEvent);
                        Path name = ev.context();
                        Path child = dir.resolve(name);

                        registerDirectoryIfCreated(watchEvent, child);

                        log.debug("{}: {}\n", ev.kind().name(), child);

                        resetAndRemoveKeyIfDirNotAccessible(key);
                        return ev;
                    })
                    .filter(this::filterPath)
                    .collect(Collectors.toList());

            batchProcess(filteredEvents);

        }
    }

    /**
     * if directory is created, and watching recursively, then
     * register it and its sub-directories
     */
    private void registerDirectoryIfCreated(WatchEvent<?> watchEvent, Path child) {
        if (recursive && (watchEvent.kind() == ENTRY_CREATE)) {
            try {
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    registerAll(child);
                }
            } catch (IOException x) {
                log.error("Exception registering directory {}.\n{}", child,
                        ExceptionUtils.getMessage(x));
            }
        }
    }

    private void resetAndRemoveKeyIfDirNotAccessible(WatchKey key) {
        boolean valid = key.reset();
        if (!valid) {
            keys.remove(key);
        }
    }


    public abstract void batchProcess(List<WatchEvent<Path>> event);

    /**
     * Implement this method to determine which paths to filter and ignore.
     *
     * @param event
     * @return
     */
    public boolean filterPath(WatchEvent<Path> event) {
        return false;
    }

}
