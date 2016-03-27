package netdava.jbakery.web.watch;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import netdava.jbakery.web.Oven;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
@Slf4j
@Component
public class BakeryWatchService {

    @Value("${source:.}")
    String source;

    @Autowired
    Oven oven;

    @PostConstruct
    public void registerFileWatcherForBakingOrders() throws IOException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        final Path path = FileSystems.getDefault().getPath(source).toAbsolutePath();
        log.info("Watching path {} for baking orders.", path);
        WatchDir watchDir = new WatchDir(oven, path, true);

        executorService.submit(new WatchWorker(oven, watchDir));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down baking thread.");
            executorService.shutdown();
        }));
    }

    @Data
    @Slf4j
    public static class WatchWorker implements Callable<Object> {
        final Oven oven;
        final WatchDir watchDir;

        @Override
        public Object call() throws Exception {
            log.info("Started file watcher");
            watchDir.processEvents();
            return null;
        }

    }

}
