import * as React from 'react';
import UploadMedia from './UploadMedia';

export type UploadViewerProps = {
  upload: DBUpload;
  media: DBMedia;
  constrained?: boolean;
  censored?: boolean;
};

export default function UploadViewer({upload, media, constrained = true, censored = false}: UploadViewerProps) {
  return (
    <div className={`MediaViewer ${constrained ? 'constrained' : ''} ${censored ? 'censored' : ''}`.trim()}>
      <div className="MediaObject">
        <UploadMedia upload={upload} media={media}/>
      </div>
    </div>
  );
}
