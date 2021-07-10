import clsx from 'clsx';
import * as React from 'react';
import useLS from '../../Hooks/useLS';
import LS_KEYS from '../../LS_KEYS';

export type UploadMediaProps = {
  upload: DBUpload;
  media: DBMedia;
  censored?: boolean;
};

const UploadMedia = React.forwardRef(({upload, media, censored = false}: UploadMediaProps, ref: React.ForwardedRef<HTMLElement>) => {
  const [defaultMuted] = useLS(LS_KEYS.DEFAULT_MUTED, true);
  const [defaultAutoplay] = useLS(LS_KEYS.DEFAULT_AUTOPLAY, true);
  const [defaultLoop] = useLS(LS_KEYS.DEFAULT_LOOP, true);

  const fullSrc = `/full/${upload.id}`;
  const censoredClass = censored ? 'censored' : null;
  const modqueued = (BigInt(upload.state) & (1n << 5n)) != 0n;

  return (
    (/^image\//i.test(media.mime) ? (
      <img className={censoredClass} ref={ref as any} src={fullSrc} alt="Rendered Upload"/>
    ) : (
      <video ref={ref as any} className={clsx('max-h-full mx-auto object-contain', censoredClass)} loop={defaultLoop} muted={defaultMuted} autoPlay={!modqueued && defaultAutoplay} controls={true}>
        <source src={fullSrc}/>
      </video>
    ))
  );
});
export default UploadMedia;
