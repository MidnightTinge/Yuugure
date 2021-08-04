import * as React from 'react';
import MediaPreview from './MediaPreview';

export type MediaPreviewBlockProps = {
  upload: RenderableUpload;
};

export const MediaPreviewBlock: React.FC<MediaPreviewBlockProps> = ({upload}: MediaPreviewBlockProps) => {
  return (
    <div className="flex flex-row">
      <div className="flex-shrink flex-grow-0">
        <MediaPreview upload={upload}/>
      </div>
      <div className="flex-shrink-0 flex-grow flex flex-col justify-center items-start">
        <table>
          <tbody>
            <tr>
              <th className="text-right pr-2">ID:</th>
              <td><a href={`/view/${upload.upload.id}`} target="_blank" className="text-blue-400 hover:text-blue-500 underline">{upload.upload.id}</a></td>
            </tr>
            <tr>
              <th className="text-right pr-2">SHA256:</th>
              <td>{upload.media.sha256}</td>
            </tr>
            <tr>
              <th className="text-right pr-2">MD5:</th>
              <td>{upload.media.md5}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default MediaPreviewBlock;
