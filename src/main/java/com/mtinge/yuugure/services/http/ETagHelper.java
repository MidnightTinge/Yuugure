package com.mtinge.yuugure.services.http;

import com.mtinge.yuugure.core.PrometheusMetrics;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.util.ETag;
import lombok.AllArgsConstructor;
import org.apache.commons.jcs3.JCS;
import org.apache.commons.jcs3.access.CacheAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

/**
 * An ETag generator that uses CRC-32 for speed and caches based on MTime.
 */
public class ETagHelper implements PathResourceManager.ETagFunction {
  private static final Logger logger = LoggerFactory.getLogger(ETagHelper.class);

  private final CacheAccess<String, CacheEntry> cache;
  private static final Object _monitor = new Object();

  public ETagHelper() {
    cache = JCS.getInstance("etag");
  }

  public ETag generate(Path path) {
    try {
      var file = path.toFile();
      var modified = Files.getLastModifiedTime(path);

      // Check the cache, if we don't get a hit or our hit is old then recompute and store.
      synchronized (_monitor) {
        var key = file.toString();
        var cached = cache.get(key);
        if (cached == null || modified.toMillis() > cached.mtime) {
          PrometheusMetrics.ETAG_CACHE_MISSES.labels(cached == null ? "empty" : "outdated").inc();
          try (var fis = new FileInputStream(file)) {
            // Compute a CRC32 hash
            var crc32 = new CRC32();

            byte[] chunk = new byte[8192];
            int read;
            while ((read = fis.read(chunk)) > 0) {
              crc32.update(chunk, 0, read);
            }

            // Get the sha256 (filename) to add onto the CRC32
            String name = file.getName();
            var liof = name.lastIndexOf('.');
            if (liof > -1) {
              name = name.substring(0, liof);
            }

            // Prepend the filename we extracted to avoid direct CRC32 collisions
            String etag = name + ";" + Long.toHexString(crc32.getValue());

            // Store the new cached object
            cached = new CacheEntry(modified.toMillis(), etag);
            cache.put(key, cached);
          }
        } else {
          PrometheusMetrics.ETAG_CACHE_HITS.inc();
        }

        return new ETag(false, cached.hash);
      }
    } catch (Exception e) {
      logger.error("Failed to generate ETag for path {}.", path.getFileName().toString(), e);
    }

    return null;
  }

  @AllArgsConstructor
  private static final class CacheEntry {
    public final long mtime;
    public final String hash;
  }
}
