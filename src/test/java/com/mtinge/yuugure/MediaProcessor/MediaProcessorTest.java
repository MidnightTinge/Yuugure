package com.mtinge.yuugure.MediaProcessor;

import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBProcessingQueue;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.processor.ProcessableUpload;
import com.mtinge.yuugure.services.processor.MediaProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;

public class MediaProcessorTest {
  @AfterEach
  void killThumb() {
    var thumb = Path.of("./volatile.thumb").toFile();
    if (thumb.exists()) {
      thumb.delete();
    }
  }

  @Test
  void handlesImage() {
    var file = Path.of("./src/test/resources/MediaProcessor/image1.png").toFile();
    Assertions.assertNotNull(file, "Failed to get test resource.");
    Assertions.assertTrue(file.exists(), "Failed to get test resource.");

    var fullPath = file.toPath();
    var thumbPath = Path.of("./volatile.thumb");

    var _queue = new DBProcessingQueue(0, 0, Timestamp.from(Instant.now()), true, false, null, false);
    var _upload = new DBUpload(0, 0, 0, 0, Timestamp.from(Instant.now()), 0L);
    var _media = new DBMedia(0, "", "", "", "image/png");
    var processable = new ProcessableUpload(_queue, _upload, _media);

    var result = MediaProcessor.Process(processable, fullPath, thumbPath);
    Assertions.assertNotNull(result);
    Assertions.assertTrue(result.success());

    Assertions.assertTrue(thumbPath.toFile().exists());

    Assertions.assertNotNull(result.meta());
    var meta = result.meta();

    Assertions.assertFalse(meta.video());
    Assertions.assertEquals(0L, meta.videoDuration());
    Assertions.assertEquals(512, meta.width());
    Assertions.assertEquals(328, meta.height());
    Assertions.assertFalse(meta.hasAudio());
  }

  @Test
  void handlesVideoWithoutAudio() {
    var file = Path.of("./src/test/resources/MediaProcessor/mute.mp4").toFile();
    Assertions.assertNotNull(file, "Failed to get test resource.");
    Assertions.assertTrue(file.exists(), "Failed to get test resource.");

    var fullPath = file.toPath();
    var thumbPath = Path.of("./volatile.thumb");

    var _queue = new DBProcessingQueue(0, 0, Timestamp.from(Instant.now()), true, false, null, false);
    var _upload = new DBUpload(0, 0, 0, 0, Timestamp.from(Instant.now()), 0L);
    var _media = new DBMedia(0, "", "", "", "video/mp4");
    var processable = new ProcessableUpload(_queue, _upload, _media);

    var result = MediaProcessor.Process(processable, fullPath, thumbPath);
    Assertions.assertNotNull(result);
    Assertions.assertTrue(result.success());

    Assertions.assertTrue(thumbPath.toFile().exists());

    Assertions.assertNotNull(result.meta());
    var meta = result.meta();

    Assertions.assertTrue(meta.video());
    Assertions.assertEquals(5L, meta.videoDuration());
    Assertions.assertEquals(240, meta.width());
    Assertions.assertEquals(120, meta.height());
    Assertions.assertFalse(meta.hasAudio());
  }

  @Test
  void handlesVideoWithAudio() {
    var file = Path.of("./src/test/resources/MediaProcessor/audio.mp4").toFile();
    Assertions.assertNotNull(file, "Failed to get test resource.");
    Assertions.assertTrue(file.exists(), "Failed to get test resource.");

    var fullPath = file.toPath();
    var thumbPath = Path.of("./volatile.thumb");

    var _queue = new DBProcessingQueue(0, 0, Timestamp.from(Instant.now()), true, false, null, false);
    var _upload = new DBUpload(0, 0, 0, 0, Timestamp.from(Instant.now()), 0L);
    var _media = new DBMedia(0, "", "", "", "video/mp4");
    var processable = new ProcessableUpload(_queue, _upload, _media);

    var result = MediaProcessor.Process(processable, fullPath, thumbPath);
    Assertions.assertNotNull(result);
    Assertions.assertTrue(result.success());

    Assertions.assertTrue(thumbPath.toFile().exists());

    Assertions.assertNotNull(result.meta());
    var meta = result.meta();

    Assertions.assertTrue(meta.video());
    Assertions.assertTrue(meta.hasAudio());
    Assertions.assertEquals(5L, meta.videoDuration());
    Assertions.assertEquals(240, meta.width());
    Assertions.assertEquals(120, meta.height());
  }
}
