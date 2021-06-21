package com.mtinge.yuugure.services.processor;

import com.github.kokorin.jaffree.LogLevel;
import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

@AllArgsConstructor
public class ProbeStreams {
  private static final Logger logger = LoggerFactory.getLogger(ProbeStreams.class);

  public final Stream video;
  public final Stream audio;
  public final Format format;

  public static ProbeStreams forPath(Path path, @Nullable String format) {
    try {
      var ffprobe = FFprobe.atPath()
        .setShowStreams(true)
        .setLogLevel(LogLevel.INFO)
        .setCountFrames(true)
        .setShowStreams(true)
        .setShowFormat(true)
        .setInput(path);

      if (format != null) {
        ffprobe.setFormat(format);
      }

      var probed = ffprobe.execute();
      Stream videoStream = null;
      Stream audioStream = null;

      for (var stream : probed.getStreams()) {
        if (audioStream == null && stream.getCodecType().equals(StreamType.AUDIO)) {
          audioStream = stream;
        }
        if (videoStream == null && stream.getCodecType().equals(StreamType.VIDEO)) {
          videoStream = stream;
        }

        if (audioStream != null && videoStream != null) {
          break;
        }
      }

      if (videoStream == null && audioStream == null) {
        logger.warn("No streams extracted for {}.", path.toString());
        return null;
      }

      return new ProbeStreams(videoStream, audioStream, probed.getFormat());
    } catch (Exception e) {
      logger.error("Failed to extract streams for path {}.", path.toString(), e);
      return null;
    }
  }
}
