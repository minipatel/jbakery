package netdava.jbakery.web;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.configuration.CompositeConfiguration;
import org.jbake.app.Asset;
import org.jbake.app.ConfigUtil;
import org.jbake.app.ContentStore;
import org.jbake.app.Crawler;
import org.jbake.app.DBUtil;
import org.jbake.app.FileUtil;
import org.jbake.app.JBakeException;
import org.jbake.app.Renderer;
import org.jbake.model.DocumentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Oven {
    private final static Logger LOGGER = LoggerFactory.getLogger(org.jbake.app.Oven.class);

    private final static Pattern TEMPLATE_DOC_PATTERN = Pattern.compile("(?:template\\.)([a-zA-Z0-9]+)(?:\\.file)");

    private CompositeConfiguration config;
    private File source;
    private File destination;
    private File templatesPath;
    private File contentsPath;
    private File assetsPath;
    private boolean isClearCache;
    private List<String> errors = new LinkedList<String>();
    private int renderedCount = 0;
    private ContentStore db;

    /**
     * Delegate c'tor to prevent API break for the moment.
     */
    public Oven(final File source, final File destination, final boolean isClearCache) throws Exception {
        this(source, destination, ConfigUtil.load(source), isClearCache);
    }

    /**
     * Creates a new instance of the Oven with references to the source and destination folders.
     *
     * @param source      The source folder
     * @param destination The destination folder
     */
    public Oven(final File source, final File destination, final CompositeConfiguration config, final boolean isClearCache) {
        this.source = source;
        this.destination = destination;
        this.config = config;
        this.isClearCache = isClearCache;
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public CompositeConfiguration getConfig() {
        return config;
    }

    // TODO: do we want to use this. Else, config could be final
    public void setConfig(final CompositeConfiguration config) {
        this.config = config;
    }


    private void ensureSource() {
        if (!FileUtil.isExistingFolder(source)) {
            throw new JBakeException("Error: Source folder must exist: " + source.getAbsolutePath());
        }
        if (!source.canRead()) {
            throw new JBakeException("Error: Source folder is not readable: " + source.getAbsolutePath());
        }
    }

    private void ensureDestination() {
        if (null == destination) {
            destination = new File(source, config.getString(ConfigUtil.Keys.DESTINATION_FOLDER));
        }
        if (!destination.exists()) {
            destination.mkdirs();
        }
        if (!destination.canWrite()) {
            throw new JBakeException("Error: Destination folder is not writable: " + destination.getAbsolutePath());
        }
    }

    /**
     * Checks source path contains required sub-folders (i.e. templates) and setups up variables for them.
     *
     * @throws JBakeException If template or contents folder don't exist
     */
    public void setupPaths() {
        ensureSource();
        templatesPath = setupRequiredFolderFromConfig(ConfigUtil.Keys.TEMPLATE_FOLDER);
        contentsPath = setupRequiredFolderFromConfig(ConfigUtil.Keys.CONTENT_FOLDER);
        assetsPath = setupPathFromConfig(ConfigUtil.Keys.ASSET_FOLDER);
        if (!assetsPath.exists()) {
            LOGGER.warn("No asset folder was found!");
        }
        ensureDestination();
    }

    private File setupPathFromConfig(String key) {
        return new File(source, config.getString(key));
    }

    private File setupRequiredFolderFromConfig(final String key) {
        final File path = setupPathFromConfig(key);
        if (!FileUtil.isExistingFolder(path)) {
            throw new JBakeException("Error: Required folder cannot be found! Expected to find [" + key + "] at: " + path.getAbsolutePath());
        }
        return path;
    }

    /**
     * All the good stuff happens in here...
     *
     * @throws JBakeException
     */
    public void bake() {
        if (db == null) {
            db = DBUtil.createDataStore(config.getString(ConfigUtil.Keys.DB_STORE), config.getString(ConfigUtil.Keys.DB_PATH));
            updateDocTypesFromConfiguration();
            DBUtil.updateSchema(db);
        }
//        try {
        final long start = new Date().getTime();
        LOGGER.info("Baking has started...");
        clearCacheIfNeeded(db);

        // process source content
        Crawler crawler = new Crawler(db, source, config);
        crawler.crawl(contentsPath);
        LOGGER.info("Content detected:");
        for (String docType : DocumentTypes.getDocumentTypes()) {
            int count = crawler.getDocumentCount(docType);
            if (count > 0) {
                LOGGER.info("Parsed {} files of type: {}", count, docType);
            }
        }

        Renderer renderer = new Renderer(db, destination, templatesPath, config);

        for (String docType : DocumentTypes.getDocumentTypes()) {
            for (ODocument document : db.getUnrenderedContent(docType)) {
                try {
                    renderer.render(DBUtil.documentToModel(document));
                    renderedCount++;
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            }
        }

        // write index file
        if (config.getBoolean(ConfigUtil.Keys.RENDER_INDEX)) {
            try {
                renderer.renderIndex(config.getString(ConfigUtil.Keys.INDEX_FILE));
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        // write feed file
        if (config.getBoolean(ConfigUtil.Keys.RENDER_FEED)) {
            try {
                renderer.renderFeed(config.getString(ConfigUtil.Keys.FEED_FILE));
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        // write sitemap file
        if (config.getBoolean(ConfigUtil.Keys.RENDER_SITEMAP)) {
            try {
                renderer.renderSitemap(config.getString(ConfigUtil.Keys.SITEMAP_FILE));
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        // write master archive file
        if (config.getBoolean(ConfigUtil.Keys.RENDER_ARCHIVE)) {
            try {
                renderer.renderArchive(config.getString(ConfigUtil.Keys.ARCHIVE_FILE));
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        // write tag files
        if (config.getBoolean(ConfigUtil.Keys.RENDER_TAGS)) {
            try {
                renderer.renderTags(crawler.getTags(), config.getString(ConfigUtil.Keys.TAG_PATH));
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        // mark docs as rendered
        for (String docType : DocumentTypes.getDocumentTypes()) {
            db.markConentAsRendered(docType);
        }
        // copy assets
        Asset asset = new Asset(source, destination, config);
        asset.copy(assetsPath);
        errors.addAll(asset.getErrors());

        LOGGER.info("Baking finished!");
        long end = new Date().getTime();
        LOGGER.info("Baked {} items in {}ms", renderedCount, end - start);
        if (errors.size() > 0) {
            LOGGER.error("Failed to bake {} item(s)!", errors.size());
        }
//        } finally {
//            db.close();
//            Orient.instance().shutdown();
//        }
    }

    /**
     * Iterates over the configuration, searching for keys like "template.index.file=..."
     * in order to register new document types.
     */
    private void updateDocTypesFromConfiguration() {
        Iterator<String> keyIterator = config.getKeys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Matcher matcher = TEMPLATE_DOC_PATTERN.matcher(key);
            if (matcher.find()) {
                DocumentTypes.addDocumentType(matcher.group(1));
            }
        }
    }

    private void clearCacheIfNeeded(final ContentStore db) {
        boolean needed = isClearCache;
        if (!needed) {
            List<ODocument> docs = db.getSignaturesForTemplates();
            String currentTemplatesSignature;
            try {
                currentTemplatesSignature = FileUtil.sha1(templatesPath);
            } catch (Exception e) {
                currentTemplatesSignature = "";
            }
            if (!docs.isEmpty()) {
                String sha1 = docs.get(0).field("sha1");
                needed = !sha1.equals(currentTemplatesSignature);
                if (needed) {
                    db.updateSignatures(currentTemplatesSignature);
                }
            } else {
                // first computation of templates signature
                db.insertSignature(currentTemplatesSignature);
                needed = true;
            }
        }
        if (needed) {
            for (String docType : DocumentTypes.getDocumentTypes()) {
                try {
                    db.deleteAllByDocType(docType);
                } catch (Exception e) {
                    // maybe a non existing document type
                }
            }
            DBUtil.updateSchema(db);
        }
    }

    public List<String> getErrors() {
        return new ArrayList<String>(errors);
    }

}
