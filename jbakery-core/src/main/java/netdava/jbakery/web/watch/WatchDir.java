package netdava.jbakery.web.watch;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import netdava.jbakery.web.Oven;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.text.MessageFormat;
import java.util.List;

@Slf4j
public class WatchDir extends AbstractWatchDir {

    Oven oven;
    List<String> pathsToIgnore;
    AntPathMatcher antPathMatcher = new AntPathMatcher(File.pathSeparator);


    public WatchDir(@NonNull Oven oven,
                    @NonNull Path dir,
                    boolean recursive,
                    @NonNull List<String> pathsToIgnore) throws IOException {
        super(dir, recursive);
        this.oven = oven;
        this.pathsToIgnore = pathsToIgnore;
    }

    @Override
    public void batchProcess(List<WatchEvent<Path>> events) {
        log.info("Processing changes {}", events);
        List<String> errors = bakeMeUp(oven);
        log.info(formatErrorMessages(errors));
    }

    private List<String> bakeMeUp(Oven oven) {
        oven.bake();
        return oven.getErrors();
    }

    private String formatErrorMessages(List<String> errors) {
        final StringBuilder msg = new StringBuilder();
        // TODO: decide, if we want the all errors here
        msg.append(MessageFormat.format("JBake failed with {0} errors:\n", errors.size()));
        int errNr = 1;
        for (final String error : errors) {
            msg.append(MessageFormat.format("{0}. {1}\n", errNr, error));
            ++errNr;
        }
        return msg.toString();
    }

    @Override
    public boolean filterPath(WatchEvent<Path> watchedPath) {
        return pathsToIgnore.stream()
                .anyMatch(event -> {
                    boolean result = antPathMatcher.match(event, watchedPath.context().toAbsolutePath().toString());
                    return result;
                });
    }
}
