import * as React from 'react';
import {useState} from 'react';
import {AutoSizer, InfiniteLoader, InfiniteLoaderChildProps, List, ListRowProps, Size} from 'react-virtualized';
import KY from '../../classes/KY';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Util from '../../classes/Util';

import MediaPreviewBlock from '../../Components/MediaPreview/MediaPreviewBlock';
import Spinner from '../../Components/Spinner';
import {UploadReducerHook} from '../../Hooks/useUploadReducer';

export type BulkPaginatedRendererProps = {
  endpoint: string;
  reducer: UploadReducerHook;
  onError?: (error: string) => void;
  onFetchState?: (fetching: boolean) => void;
  setMax?: (max: number) => void;
};

export const BulkPaginatedRenderer: React.FC<BulkPaginatedRendererProps> = ({endpoint, reducer, onError: _onError, onFetchState: _onFetchState, setMax: _setMax}: BulkPaginatedRendererProps) => {
  const [{uploads}, /* ignored */, uploadActions] = reducer;
  const [bottomed, setBottomed] = useState(false);
  const [fetching, setFetching] = useState(false);
  const [lastMax, setLastMax] = useState<number>(null);

  const onError = typeof _onError === 'function' ? _onError : noop;
  const onFetchState = typeof _onFetchState === 'function' ? _onFetchState : noop;
  const setMax = typeof _setMax === 'function' ? _setMax : noop;

  const rowCount = bottomed ? uploads.length : uploads.length + 1;
  const isRowLoaded = ({index}: any) => bottomed || index < uploads.length;

  async function loadMoreRows() {
    if (fetching || bottomed) {
      return;
    }

    onFetchState(true);
    setFetching(true);
    let url = endpoint;
    if (uploads.length > 0) {
      url += `?before=${uploads[uploads.length - 1].upload.id}`;
    }

    try {
      const data = await KY.get(url).json<RouterResponse>();
      const res = RouterResponseConsumer<BulkPaginatedResponse>(data);

      if (res.success) {
        onError(null);
        const [data] = res.data;
        if (data.max !== lastMax) {
          setLastMax(data.max);
          setMax(data.max);
        }

        if (data.uploads.uploads.length > 0) {
          uploadActions.add([...Util.mapBulkUploads(data.uploads)]);
        } else {
          setBottomed(true);
        }

      } else if (res.code != 404) {
        onError(res.message);
      }
    } catch (err) {
      console.error('Failed to get BulkPaginatedResponse for endpoint %o', endpoint, err);
      onError(err?.toString());
    } finally {
      setFetching(false);
      onFetchState(false);
    }
  }

  function rowRenderer({key, index, style}: ListRowProps): React.ReactNode {
    return (
      <div key={key} style={style}>
        {isRowLoaded({index}) ? (
          <MediaPreviewBlock upload={uploads[index]}/>
        ) : (
          <div className="flex flex-col justify-center items-center">
            <Spinner inline/> Loading...
          </div>
        )}
      </div>
    );
  }

  return (
    <InfiniteLoader loadMoreRows={loadMoreRows} isRowLoaded={isRowLoaded} rowCount={rowCount} threshold={1} minimumBatchSize={1}>
      {({onRowsRendered, registerChild}: InfiniteLoaderChildProps) => (
        <AutoSizer>
          {({width, height}: Size) => (
            <List ref={registerChild} onRowsRendered={onRowsRendered} rowCount={rowCount} rowRenderer={rowRenderer} rowHeight={250} width={width} height={height}/>
          )}
        </AutoSizer>
      )}
    </InfiniteLoader>
  );
};

export default BulkPaginatedRenderer;

function noop() {
  //
}
