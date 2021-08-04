import * as React from 'react';
import UploadMedia from './UploadMedia';

export type UploadViewerProps = {
  upload: DBUpload;
  media: DBMedia;
  constrained?: boolean;
  censored?: boolean;
};

export const UploadViewer: React.FC<UploadViewerProps> = ({upload, media, constrained = true, censored = false}: UploadViewerProps) => {
  return (
    <div className={`MediaViewer ${constrained ? 'constrained' : ''}`.trim()}>
      <div className="MediaObject">
        <UploadMedia upload={upload} media={media} censored={censored}/>
      </div>
    </div>
  );
};

export default UploadViewer;
