package com.mtinge.yuugure.services.processor;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.Filter;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.TagManager.TagCategory;
import com.mtinge.yuugure.core.ThreadFactories;
import com.mtinge.yuugure.data.processor.MediaMeta;
import com.mtinge.yuugure.data.processor.ProcessableUpload;
import com.mtinge.yuugure.data.processor.ProcessorResult;
import com.mtinge.yuugure.services.IService;
import com.mtinge.yuugure.services.messaging.Messaging;
import lombok.Getter;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.io.BasicOutputBuffer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>The MediaProcessor's job is to take an upload and perform tasks that require external tools
 * such as thumbnail generation and metadata tagging. Uses FFMPEG and FFPROBE extensively for
 * tagging and validation.</p>
 * <p>MediaProcessor is purposefully written to be naïve since it is possible for a processor to
 * have died part-way through. It is also possible for a half/already-finished processor to be
 * started again via DB manipulation. The process should never fail just because an unexpected path
 * exists (e.g. thumbnail), and no preexisting state should be misconstrued as evidence of a
 * previously successful run.</p>
 * <p>
 * To facilitate this statelessness, the processor is destructive. Existing files will be
 * overwritten because it is unknown if the processor died half way through writing, and even if it
 * didn't, we explicitly want them replaced if we've restarted a processor. This also facilitates
 * re-processing existing uploads in the case of the processor being updated to add new tags.
 * </p>
 */
public class MediaProcessor implements IService {
  private static final Logger logger = LoggerFactory.getLogger(MediaProcessor.class);

  @Getter
  ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), ThreadFactories.prefixed("MediaProcessor/"));
  @Getter
  LinkedList<Worker> workers = new LinkedList<>();

  public MediaProcessor() {
    //
  }

  @Override
  public void init() throws Exception {
    // Sockets bind immediately when creating a worker so we do initialization directly in start().
  }

  @Override
  public void start() throws Exception {
    var context = App.messaging().context();
    var bindIncoming = App.config().zeromq.bind.internalSupplier;
    var bindOutgoing = App.config().zeromq.bind.internalConsolidator;

    for (var i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
      var worker = new Worker(context, bindIncoming, bindOutgoing);
      workers.add(worker);
      executor.submit(worker);
    }
  }

  @Override
  public void stop() throws Exception {
    for (var worker : workers) {
      worker.close();
      worker.wake();
    }
  }

  public void wakeWorkers() {
    for (var worker : workers) {
      worker.wake();
    }
  }

  public static ProcessorResult Process(ProcessableUpload dequeued, Path fullPath, Path thumbPath) {
    if (dequeued == null) return null;

    logger.debug("Beginning processing of {}", dequeued.media.sha256);

    // Harvest metadata
    var result = new ProcessorResult(dequeued);
    var isJpeg = dequeued.media.mime.toLowerCase().endsWith("jpeg");

    // ffprobe has a hard time with the jpeg so we'll set the format specifically if needed
    var streams = ProbeStreams.forPath(fullPath, isJpeg ? "mjpeg" : null);
    if (streams != null) {
      // Images are reported as a video codec, so we should always have a "video" stream. If we
      // don't, user uploaded an unprocessable file as we can't extract a thumbnail.
      if (streams.video != null) {
        var meta = new MediaMeta(dequeued.media.id)
          .width(streams.video.getWidth())
          .height(streams.video.getHeight())
          .video(dequeued.media.mime.toLowerCase().startsWith("video/"))
          .videoDuration(Optional.ofNullable(Optional.ofNullable(streams.video.getDuration()).orElse(streams.format.getDuration())).orElse(0f)) // try to extract from the video stream first, then the format specifier
          .hasAudio(false) // audio detection is handled with an ffmpeg filter later.
          .fileSize(streams.format.getSize());

        if (streams.audio != null) {
          var volumeData = VolumeData.detect(fullPath);
          if (volumeData != null) {
            meta.hasAudio(volumeData.meanVolume > -80 || volumeData.maxVolume > -80);
          } // else: invalid audio stream, no detectable volume
        }

        // ensure we attach our metadata
        result.meta(meta);

        // Create thumbnail
        if (createThumbnail(fullPath, thumbPath, meta.video() ? ((long) Math.floor(meta.videoDuration() / 8)) : 0, isJpeg ? "mjpeg" : null)) {
          // TODO automated tagging should happen here when tagging is implemented.
          var tags = new LinkedList<String>();

          // Automated Tagging
          //  Meta
          if (meta.video()) {
            tags.add("meta:video");
          }
          if (meta.hasAudio()) {
            tags.add("meta:has_audio");
          }

          //  Filesize
          var fs = FileSize.get(streams.format.getSize().intValue(), meta.video());
          if (fs != null) {
            tags.add(TagCategory.FILESIZE.getName() + ":" + fs);
          }

          //  Dimensions
          var dm = FileDimension.get(meta.width() * meta.height());
          if (dm != null) {
            tags.add(TagCategory.DIMENSIONS.getName() + ":" + dm);
          }

          //  Length
          if (meta.video()) {
            var fl = FileLengths.get(meta.videoDuration());
            if (fl != null) {
              tags.add(TagCategory.LENGTH.getName() + ":" + fl);
            }
          }

          return result.tags(tags).success(true);
        } else {
          // else: failed to create thumbnail
          return result.success(false).message("Thumbnail generation failed.");
        }
      } else {
        // else: invalid file, no video streams
        return result.success(false).message("No video streams.");
      }
    } else {
      // else: invalid file, no streams
      return result.success(false).message("No video/audio streams.");
    }
  }

  public static boolean createThumbnail(Path fullPath, Path thumbPath, @Nullable Long seek, @Nullable String format) {
    if (seek == null) {
      seek = 0L;
    }

    try {
      var input = UrlInput.fromPath(fullPath).setPosition(seek);
      var ffmpeg = FFmpeg.atPath()
        .addInput(input)
        .setFilter(StreamType.VIDEO, Filter.withName("scale")
          .addArgumentEscaped("'if(gt(a,200/200),200,-1)':'if(gt(a,200/200),-1,200)'")
        )
        .addArguments("-sws_flags", "bicubic+full_chroma_inp")
        .setOverwriteOutput(true)
        .addOutput(
          UrlOutput.toPath(thumbPath)
            .setFrameCount(StreamType.VIDEO, 1L)
            .setFormat("apng")
        );
      if (format != null) {
        input.setFormat(format);
      }
      ffmpeg.execute();

      return true;
    } catch (Exception e) {
      logger.error("Failed to create thumbnail for path {}.", fullPath, e);
      return false;
    }
  }

  private static final class Worker implements Runnable, Closeable {
    private static final Logger logger = LoggerFactory.getLogger("MediaProcessor/Worker");

    // NOTE: The monitor is only used for wait/notify, data synchronization already happens on two
    //       levels - first on a DB level with row locks and second on a functional level within
    //       the work supplier in the Messaging class. This already may be too much synchronization
    //       so I'm hesitant to add a third layer, however if it's necessary in the future it will
    //       be an easy addition.
    private final Object monitor = new Object();
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private final AtomicBoolean closeCalled = new AtomicBoolean(false);
    private final ZMQ.Socket socketIncoming;
    private final ZMQ.Socket socketOutgoing;

    public Worker(ZContext context, String bindIncoming, String bindOutgoing) {
      this.socketIncoming = context.createSocket(SocketType.REQ);
      this.socketOutgoing = context.createSocket(SocketType.PUSH);

      if (!this.socketIncoming.connect(bindIncoming)) {
        throw new IllegalStateException("Failed to bind the incoming socket.");
      }

      if (!this.socketOutgoing.connect(bindOutgoing)) {
        throw new IllegalStateException("Failed to bind the outgoing socket.");
      }
    }

    @Override
    public void run() {
      while (!shutdownRequested.get()) {
        if (closeCalled.get()) {
          return;
        }

        // get work
        if (!socketIncoming.send(new byte[]{1})) {
          logger.warn("Failed to request work");
        } else {
          var received = socketIncoming.recv(0);
          if (received == null) {
            continue;
          }

          boolean shouldSleep = received.length == 1;
          if (received.length == 1) {
            // got an opcode
            if (received[0] == Messaging.NOT_ACCEPTABLE) {
              throw new IllegalStateException("Received the NOT_ACCEPTABLE response to a work payload request. Cannot continue.");
            }
          } else {
            var parsed = ProcessableUpload.readFrom(new BsonBinaryReader(ByteBuffer.wrap(received)));
            if (parsed != null) {
              logger.info("Received job for upload {}.", parsed.upload.id);
              var fullPath = Path.of(App.config().upload.finalDir, parsed.media.sha256 + ".full");
              var thumbPath = Path.of(App.config().upload.finalDir, parsed.media.sha256 + ".thumb");
              var result = MediaProcessor.Process(parsed, fullPath, thumbPath);

              var bob = new BasicOutputBuffer();
              var writer = new BsonBinaryWriter(bob);
              result.writeTo(writer);

              byte[] bytes = null;
              try (var bos = new ByteArrayOutputStream()) {
                bob.pipe(bos);
                bytes = bos.toByteArray();
              } catch (Exception e) {
                logger.error("Failed to serialize result for upload {}.", parsed.upload.id, e);
              }

              if (bytes != null) {
                socketOutgoing.send(bytes);
              }
            } else {
              shouldSleep = true;
              logger.error("Received invalid ProcessableUpload. Processing has been skipped.");
            }
          }

          if (shouldSleep) {
            synchronized (monitor) {
              // We didn't get work so wait for either a notification or for 30s to pass before
              // trying again.
              try {
                monitor.wait(30000);
              } catch (InterruptedException ie) {
                // ignored - this can happen for a multitude of reasons, one of which being a notify
              }
            }
          }
        }
      }
    }

    public void wake() {
      synchronized (monitor) {
        monitor.notify();
      }
    }

    @Override
    public void close() throws IOException {
      if (closeCalled.compareAndSet(false, true)) {
        if (this.socketIncoming != null) {
          this.socketIncoming.close();
        }
        if (this.socketOutgoing != null) {
          this.socketOutgoing.close();
        }
      }
    }
  }
}
