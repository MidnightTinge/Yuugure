import * as React from 'react';
import {AutoSizer, List, ListRowProps, Size} from 'react-virtualized';
import MediaPreviewBlock from '../../Components/MediaPreview/MediaPreviewBlock';

export type UploadsRendererProps = {
  uploads: RenderableUpload[];
  errored: boolean;
};

export default function UploadsRenderer({uploads, errored}: UploadsRendererProps) {
  function rowRenderer({key, index, style}: ListRowProps): React.ReactNode {
    return (
      <div key={key} style={style}>
        <MediaPreviewBlock upload={uploads[index]}/>
      </div>
    );
  }

  return (
    <div className="h-full">
      {uploads && uploads.length ? (
        <>
          {errored ? (
            <p className="text-red-500 whitespace-pre-wrap">Failed to fetch uploads. Please try again later.</p>
          ) : null}
          <AutoSizer>
            {({width, height}: Size) => (
              <List rowCount={uploads.length} rowHeight={250} width={width} height={height} rowRenderer={rowRenderer}/>
            )}
          </AutoSizer>
        </>
      ) : (
        errored ? (
          <p className="text-red-500 whitespace-pre-wrap">Failed to fetch uploads. Please try again later.</p>
        ) : null
      )}
    </div>
  );
}
