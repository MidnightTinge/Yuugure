import * as React from 'react';
import {useRef} from 'react';
import useLS from '../Hooks/useLS';
import LS_KEYS from '../LS_KEYS';

export type UploadRendererProps = {
  upload: DBUpload;
  media: DBMedia;
  constrained?: boolean;
};

export default function UploadRenderer({upload, media, constrained}: UploadRendererProps) {
  const [defaultMuted] = useLS(LS_KEYS.DEFAULT_MUTED, true);
  const [defaultAutoplay] = useLS(LS_KEYS.DEFAULT_AUTOPLAY, true);
  const [defaultLoop] = useLS(LS_KEYS.DEFAULT_LOOP, true);

  const ref = useRef(null);

  let node: React.ReactElement;
  const fullRef = `/full/${upload.id}`;

  if (/^image\//i.test(media.mime)) {
    node = (
      <img ref={ref} src={fullRef} className="max-h-full mx-auto object-contain" alt="Rendered image"/>
    );
  } else {
    node = (
      <video className="max-h-full mx-auto object-contain" ref={ref} loop={defaultLoop} muted={defaultMuted} autoPlay={defaultAutoplay} controls={true}>
        <source src={fullRef}/>
      </video>
    );
  }

  return (
    <div className="inline">
      {node}
    </div>
  );
}
