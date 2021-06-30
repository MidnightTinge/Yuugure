package com.mtinge.yuugure.data;

import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBProcessingQueue;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.processor.MediaMeta;
import com.mtinge.yuugure.data.processor.ProcessableUpload;
import com.mtinge.yuugure.data.processor.ProcessorResult;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.io.BasicOutputBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@DisplayName("MediaProcessor BSON Converters")
public class BsonConversionTests {
  @Test
  @DisplayName("ProcessableUpload round-tripping")
  void ProcessableUploadRoundTrip() throws Exception {
    var media = new DBMedia(1, "sha256", "md5", "phash", "mime");
    var upload = new DBUpload(1, media.id, 0, 1, Timestamp.from(Instant.now().minus(Duration.ofDays(2))), States.Upload.MODERATION_QUEUED);
    var processingQueue = new DBProcessingQueue(1, upload.id, Timestamp.from(upload.uploadDate.toInstant().plusSeconds(1)), true, false, null, false);
    var processable = new ProcessableUpload(processingQueue, upload, media);

    var bob = new BasicOutputBuffer();
    var writer = new BsonBinaryWriter(bob);
    processable.writeTo(writer);

    byte[] bytes;
    try (var bos = new ByteArrayOutputStream()) {
      bob.pipe(bos);
      bytes = bos.toByteArray();
    }
    Assertions.assertNotEquals(0, bytes.length);

    var reader = new BsonBinaryReader(ByteBuffer.wrap(bytes));
    var deserialized = ProcessableUpload.readFrom(reader);

    // Ensure none null
    Assertions.assertNotNull(deserialized);
    Assertions.assertNotNull(deserialized.media);
    Assertions.assertNotNull(deserialized.upload);
    Assertions.assertNotNull(deserialized.queueItem);

    // Ensure the round-trip correctly re-encoded all of the values
    Assertions.assertEquals(deserialized.media, media);
    Assertions.assertEquals(deserialized.upload, upload);
    Assertions.assertEquals(deserialized.queueItem, processingQueue);
  }

  @Test
  @DisplayName("ProcessorResult writer throws for null meta")
  void ProcessorResultNullMeta() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      var bob = new BasicOutputBuffer();
      var writer = new BsonBinaryWriter(bob);
      new ProcessorResult.Builder().build().writeTo(writer);
    });
  }

  @Test
  @DisplayName("ProcessorResult round-tripping")
  void ProcessorResultRoundTrip() throws Exception {
    var media = new DBMedia(1, "sha256", "md5", "phash", "mime");
    var upload = new DBUpload(1, media.id, 0, 1, Timestamp.from(Instant.now().minus(Duration.ofDays(2))), States.Upload.MODERATION_QUEUED);
    var processingQueue = new DBProcessingQueue(1, upload.id, Timestamp.from(upload.uploadDate.toInstant().plusSeconds(1)), true, false, null, false);
    var processable = new ProcessableUpload(processingQueue, upload, media);

    var meta = new MediaMeta.Builder()
      .media(media.id)
      .width(1920)
      .height(1080)
      .video(false)
      .hasAudio(false)
      .videoDuration(0)
      .build();
    var result = new ProcessorResult.Builder()
      .dequeued(processable)
      .meta(meta)
      .message("An internal server error occurred.")
      .success(false)
      .tags(List.of("one", "two", "three"))
      .build();

    var bob = new BasicOutputBuffer();
    var writer = new BsonBinaryWriter(bob);
    result.writeTo(writer);

    byte[] bytes;
    try (var bos = new ByteArrayOutputStream()) {
      bob.pipe(bos);
      bytes = bos.toByteArray();
    }
    Assertions.assertNotEquals(0, bytes.length);

    var reader = new BsonBinaryReader(ByteBuffer.wrap(bytes));
    var deserialized = ProcessorResult.readFrom(reader);

    Assertions.assertNotNull(deserialized);
    Assertions.assertNotNull(deserialized.meta());
    Assertions.assertNotNull(deserialized.dequeued());
    Assertions.assertNotNull(deserialized.message());
    Assertions.assertNotNull(deserialized.tags());

    Assertions.assertFalse(deserialized.tags().isEmpty());
    Assertions.assertEquals(3, deserialized.tags().size());
    Assertions.assertEquals("one", deserialized.tags().remove(0));
    Assertions.assertEquals("two", deserialized.tags().remove(0));
    Assertions.assertEquals("three", deserialized.tags().remove(0));

    Assertions.assertEquals(deserialized, result);
  }
}
