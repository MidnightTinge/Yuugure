package com.mtinge.yuugure.services.processor;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.Filter;
import com.github.kokorin.jaffree.ffmpeg.NullOutput;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@AllArgsConstructor
public class VolumeData {
  private static final Logger logger = LoggerFactory.getLogger(VolumeData.class);

  public final double meanVolume;
  public final double maxVolume;

  public static VolumeData detect(Path fullPath) {
    try {
      var lines = new LinkedList<String>();
      FFmpeg.atPath()
        .addInput(
          UrlInput.fromPath(fullPath)
        )
        .setFilter(StreamType.AUDIO, Filter.withName("volumedetect"))
        .addOutput(
          new NullOutput(false)
            .setDuration(60, TimeUnit.SECONDS)
        )
        .setOutputListener(line -> {
          if (line.startsWith("[Parsed_volumedetect")) {
            lines.add(line);
          }
        })
        .execute();

      // collect output into KVPs for processing
      var vars = lines.stream()
        .map(line -> (line.substring(line.lastIndexOf(']') + 1)).split(":"))
        .collect(Collectors.toMap(strings -> strings[0].trim(), strings -> strings[1].trim()));

      double meanVolume = -91L;
      double maxVolume = -91L;

      // process the vars we need for the MediaProcessor
      if (vars.containsKey("mean_volume") && vars.get("mean_volume").contains(" dB")) {
        meanVolume = Double.parseDouble(vars.get("mean_volume").split(" ")[0]);
      }
      if (vars.containsKey("max_volume") && vars.get("max_volume").contains(" dB")) {
        maxVolume = Double.parseDouble(vars.get("max_volume").split(" ")[0]);
      }

      return new VolumeData(meanVolume, maxVolume);
    } catch (Exception e) {
      logger.error("Failed to extract VolumeData for path {}.", fullPath, e);
    }
    return null;
  }
}
