import * as React from 'react';


export type MediaPreviewProps = {
  upload: RenderableUpload;
};

export default function MediaPreview({upload}: MediaPreviewProps) {
  const isImage = /^image\//i.test(upload.media.mime);

  return (
    <div className="MediaPreview" data-upload={upload.upload.id} data-media={upload.media.id} data-sha256={upload.media.sha256} data-md5={upload.media.md5} data-mime={upload.media.mime}>
      <div className="ThumbWrapper">
        <a href={`/view/${upload.upload.id}`} target="_blank" className={upload.state.MODERATION_QUEUED ? 'blurred hover-clear' : ''}>
          <img src={`/thumb/${upload.upload.id}`} alt={`Thumbnail for upload ${upload.upload.id}`}/>
        </a>
        <div className="IconsWrapper">
          <div className="OverlayIcon" title={`This upload is a${isImage ? 'n image' : 'video'}`}>
            <i className={`fas ${isImage ? 'fa-image' : 'fa-video'}`}/>
          </div>
          {upload.state.PRIVATE ? (
            <div className="OverlayIcon" title="This upload is private.">
              <i className="fas fa-lock"/>
            </div>
          ) : null}
          {upload.state.MODERATION_QUEUED ? (
            <div className="OverlayIcon" title="This upload is awaiting moderator approval.">
              <i className="fas fa-exclamation-circle"/>
            </div>
          ) : null}
        </div>
      </div>
      <div className="DetailsWrapper">
        <div className="Detail" title="Public Favorites" data-detail="favorites" data-detail-value={0/* TODO */}>
          <i className="fas fa-heart"/>
          <span className="text-pink-500">0</span>
        </div>
        <div className="Detail" title="Upvotes" data-detail="downvotes" data-detail-value={0/* TODO */}>
          <i className="fas fa-chevron-up"/>
          <span className="text-green-500">0</span>
        </div>
        <div className="Detail" title="Downvotes" data-detail="downvotes" data-detail-value={0/* TODO */}>
          <i className="fas fa-chevron-down"/>
          <span className="text-red-500">0</span>
        </div>
      </div>
    </div>
  );
}
