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

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(result).isNotNull();
    assertThat(result.success()).isTrue();

    assertThat(thumbPath.toFile()).exists();

    assertThat(result.meta()).isNotNull();

    var meta = result.meta();
    assertThat(meta.video()).isFalse();
    assertThat(meta.hasAudio()).isFalse();
    assertThat(meta.videoDuration()).isEqualTo(0D);
    assertThat(meta.width()).isEqualTo(512);
    assertThat(meta.height()).isEqualTo(328);
    assertThat(meta.fileSize()).isEqualTo(3655);
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
    assertThat(result).isNotNull();
    assertThat(result.success()).isTrue();

    assertThat(thumbPath.toFile()).exists();

    assertThat(result.meta()).isNotNull();

    var meta = result.meta();
    assertThat(meta.video()).isTrue();
    assertThat(meta.hasAudio()).isFalse();
    assertThat(meta.videoDuration()).isEqualTo(5D);
    assertThat(meta.width()).isEqualTo(240);
    assertThat(meta.height()).isEqualTo(120);
    assertThat(meta.fileSize()).isEqualTo(3283);
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
    assertThat(result).isNotNull();
    assertThat(result.success()).isTrue();

    assertThat(thumbPath.toFile()).exists();

    assertThat(result.meta()).isNotNull();

    var meta = result.meta();
    assertThat(meta.video()).isTrue();
    assertThat(meta.hasAudio()).isTrue();
    assertThat(meta.videoDuration()).isEqualTo(5D);
    assertThat(meta.width()).isEqualTo(240);
    assertThat(meta.height()).isEqualTo(120);
    assertThat(meta.fileSize()).isEqualTo(72142);
  }
}
