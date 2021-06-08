package com.mtinge.yuugure.data.http.views;

import java.util.Map;

public class IndexView extends ViewAndModel<IndexView.IndexModel> {
  public IndexView(boolean isAuthed) {
    super("index", new IndexModel(isAuthed));
  }

  public static final class IndexModel implements IModel {
    private final boolean isAuthed;

    private IndexModel(boolean isAuthed) {
      this.isAuthed = isAuthed;
    }

    @Override
    public Map<String, Object> toMap() {
      return Map.of("authed", isAuthed);
    }
  }
}
