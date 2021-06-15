import * as React from 'react';
import useLS from '../../Hooks/useLS';
import LS_KEYS from '../../LS_KEYS';

export type UploadMediaProps = {
  upload: DBUpload;
  media: DBMedia;
};

const UploadMedia = React.forwardRef(({upload, media}: UploadMediaProps, ref: React.ForwardedRef<HTMLElement>) => {
  const [defaultMuted] = useLS(LS_KEYS.DEFAULT_MUTED, true);
  const [defaultAutoplay] = useLS(LS_KEYS.DEFAULT_AUTOPLAY, true);
  const [defaultLoop] = useLS(LS_KEYS.DEFAULT_LOOP, true);

  const fullSrc = `/full/${upload.id}`;

  return (
    (/^image\//i.test(media.mime) ? (
      <img ref={ref as any} src={fullSrc} alt="Rendered Upload"/>
    ) : (
      <video ref={ref as any} className="max-h-full mx-auto object-contain" loop={defaultLoop} muted={defaultMuted} autoPlay={defaultAutoplay} controls={true}>
        <source src={fullSrc}/>
      </video>
    ))
  );
});
export default UploadMedia;
