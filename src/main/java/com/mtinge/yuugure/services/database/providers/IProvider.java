package com.mtinge.yuugure.services.database.providers;

import com.mtinge.yuugure.services.database.results.Result;
import org.jdbi.v3.core.Handle;

public interface IProvider<DBTYPE, CREATOR> {
  Result<DBTYPE> create(CREATOR props, Handle handle);

  DBTYPE read(int id, Handle handle);

  Result<DBTYPE> update(int id, CREATOR updated, Handle handle);

  Result<DBTYPE> delete(int id, Handle handle);
}
