package netdava.jbakery.web.watch;

import lombok.extern.slf4j.Slf4j;
import netdava.jbakery.web.Oven;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.text.MessageFormat;
import java.util.List;

@Slf4j
public class WatchDir extends AbstractWatchDir {

    Oven oven;

    public WatchDir(Oven oven, Path dir, boolean recursive) throws IOException {
        super(dir, recursive);
        this.oven = oven;
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

}
