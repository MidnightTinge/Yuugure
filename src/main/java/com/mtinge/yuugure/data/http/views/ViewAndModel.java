package com.mtinge.yuugure.data.http.views;

import java.util.Map;

public abstract class ViewAndModel<MODEL extends IModel> {
  private String view;
  private MODEL model;

  public ViewAndModel(String view, MODEL model) {
    this.view = view;
    this.model = model;
  }

  public String getView() {
    return view;
  }

  public Map<String, Object> getModel() {
    return model.toMap();
  }
}
