package netdava.jbakery.web.watch;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import netdava.jbakery.web.Oven;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
@Slf4j
@Component
public class BakeryWatchService {

    @Value("${source:.}")
    String source;

    @Value("${destination:output}")
    String destination;

    @Autowired
    Oven oven;

    @Autowired
    Environment env;

    @PostConstruct
    public void registerFileWatcherForBakingOrders() throws IOException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        final Path path = FileSystems.getDefault().getPath(source).toAbsolutePath();
        log.info("Watching path {} for baking orders.", path);

        List<String> pathsToIgnore = pathsToIgnore();
        WatchDir watchDir = new WatchDir(oven, path, true, pathsToIgnore);

        executorService.submit(new WatchWorker(oven, watchDir));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down baking thread.");
            executorService.shutdown();
        }));
    }

    private List<String> pathsToIgnore() {
        List<String> pathsToIgnore = new ArrayList<>();
        if (env.containsProperty("ignore")) {
            String ignored = env.getProperty("ignore");
            pathsToIgnore.addAll(Arrays.asList(ignored.split(":")));
        } else {
            pathsToIgnore.add(String.format("%s/**", destination));
        }
        return pathsToIgnore;
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
