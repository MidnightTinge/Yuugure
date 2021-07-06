package com.mtinge.yuugure.MediaProcessor;

import com.mtinge.yuugure.services.processor.FileSize;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSizeTest {
  @Nested
  public final class VideoSize {
    @Test
    void tiny() {
      assertThat(FileSize.get(0, true)).isEqualTo(FileSize.VIDEO_TINY.name);
      assertThat(FileSize.get(FileSize.MB * 5 - 1, true)).isEqualTo(FileSize.VIDEO_TINY.name);
      assertThat(FileSize.get(FileSize.MB * 5, true)).isNotEqualTo(FileSize.VIDEO_TINY.name);
    }

    @Test
    void small() {
      assertThat(FileSize.get(FileSize.MB * 5, true)).isEqualTo(FileSize.VIDEO_SMALL.name);
      assertThat(FileSize.get(FileSize.MB * 15 - 1, true)).isEqualTo(FileSize.VIDEO_SMALL.name);
      assertThat(FileSize.get(FileSize.MB * 15, true)).isNotEqualTo(FileSize.VIDEO_SMALL.name);
    }

    @Test
    void medium() {
      assertThat(FileSize.get(FileSize.MB * 15, true)).isEqualTo(FileSize.VIDEO_MEDIUM.name);
      assertThat(FileSize.get(FileSize.MB * 30 - 1, true)).isEqualTo(FileSize.VIDEO_MEDIUM.name);
      assertThat(FileSize.get(FileSize.MB * 30, true)).isNotEqualTo(FileSize.VIDEO_MEDIUM.name);
    }

    @Test
    void large() {
      assertThat(FileSize.get(FileSize.MB * 30, true)).isEqualTo(FileSize.VIDEO_LARGE.name);
      assertThat(FileSize.get(FileSize.MB * 50 - 1, true)).isEqualTo(FileSize.VIDEO_LARGE.name);
      assertThat(FileSize.get(FileSize.MB * 50, true)).isNotEqualTo(FileSize.VIDEO_LARGE.name);
    }

    @Test
    void massive() {
      assertThat(FileSize.get(FileSize.MB * 50, true)).isEqualTo(FileSize.VIDEO_MASSIVE.name);
      assertThat(FileSize.get(FileSize.MB * 100, true)).isEqualTo(FileSize.VIDEO_MASSIVE.name);
    }
  }

  @Nested
  public final class ImageSize {
    @Test
    void tiny() {
      assertThat(FileSize.get(0, false)).isEqualTo(FileSize.IMAGE_TINY.name);
      assertThat(FileSize.get(FileSize.MB - 1, false)).isEqualTo(FileSize.IMAGE_TINY.name);
      assertThat(FileSize.get(FileSize.MB, false)).isNotEqualTo(FileSize.IMAGE_TINY.name);
    }

    @Test
    void small() {
      assertThat(FileSize.get(FileSize.MB, false)).isEqualTo(FileSize.IMAGE_SMALL.name);
      assertThat(FileSize.get(FileSize.MB * 5 - 1, false)).isEqualTo(FileSize.IMAGE_SMALL.name);
      assertThat(FileSize.get(FileSize.MB * 5, false)).isNotEqualTo(FileSize.IMAGE_SMALL.name);
    }

    @Test
    void medium() {
      assertThat(FileSize.get(FileSize.MB * 5, false)).isEqualTo(FileSize.IMAGE_MEDIUM.name);
      assertThat(FileSize.get(FileSize.MB * 15 - 1, false)).isEqualTo(FileSize.IMAGE_MEDIUM.name);
      assertThat(FileSize.get(FileSize.MB * 15, false)).isNotEqualTo(FileSize.IMAGE_MEDIUM.name);
    }

    @Test
    void large() {
      assertThat(FileSize.get(FileSize.MB * 15, false)).isEqualTo(FileSize.IMAGE_LARGE.name);
      assertThat(FileSize.get(FileSize.MB * 30 - 1, false)).isEqualTo(FileSize.IMAGE_LARGE.name);
      assertThat(FileSize.get(FileSize.MB * 30, false)).isNotEqualTo(FileSize.IMAGE_LARGE.name);
    }

    @Test
    void massive() {
      assertThat(FileSize.get(FileSize.MB * 30, false)).isEqualTo(FileSize.IMAGE_MASSIVE.name);
      assertThat(FileSize.get(FileSize.MB * 100, false)).isEqualTo(FileSize.IMAGE_MASSIVE.name);
    }
  }
}
