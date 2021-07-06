import * as React from 'react';
import {useHistory} from 'react-router-dom';
import Util from '../../classes/Util';


export type MediaPreviewProps = {
  upload: RenderableUpload;
};

export default function MediaPreview({upload}: MediaPreviewProps) {
  const history = useHistory();
  const isImage = /^image\//i.test(upload.media.mime);

  function handleNavigate(e: React.MouseEvent<HTMLAnchorElement>) {
    e.preventDefault();
    history.push(`/view/${upload.upload.id}`);
  }

  return (
    <div className="MediaPreview" data-upload={upload.upload.id} data-media={upload.media.id} data-sha256={upload.media.sha256} data-md5={upload.media.md5} data-mime={upload.media.mime}>
      <div className="ThumbWrapper">
        <a href={`/view/${upload.upload.id}`} onClick={handleNavigate} className={upload.state.MODERATION_QUEUED ? 'blurred hover-clear' : ''}>
          <img src={`/thumb/${upload.upload.id}`} alt={`Thumbnail for upload ${upload.upload.id}`} className={Util.joinedClassName('thumbnail', upload.state.MODERATION_QUEUED ? 'censored hover-clear' : null)}/>
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
              <i className="fas fa-exclamation-triangle"/>
            </div>
          ) : null}
        </div>
      </div>
      <div className="DetailsWrapper">
        <div className={Util.joinedClassName('Detail', upload.bookmarks.bookmarked ? 'text-pink-500' : null)} title="Public Favorites" data-detail="favorites" data-detail-value={upload.bookmarks.total_public}>
          <i className="fas fa-heart"/>
          <span className="text-pink-500">{upload.bookmarks.total_public}</span>
        </div>
        <div className={Util.joinedClassName('Detail', (upload.votes.voted && upload.votes.is_upvote) ? 'text-green-500' : null)} title="Upvotes" data-detail="downvotes" data-detail-value={upload.votes.total_upvotes}>
          <i className="fas fa-chevron-up"/>
          <span className="text-green-500">{upload.votes.total_upvotes}</span>
        </div>
        <div className={Util.joinedClassName('Detail', (upload.votes.voted && !upload.votes.is_upvote) ? 'text-red-500' : null)} title="Downvotes" data-detail="downvotes" data-detail-value={upload.votes.total_downvotes}>
          <i className="fas fa-chevron-down"/>
          <span className="text-red-500">{upload.votes.total_downvotes}</span>
        </div>
      </div>
    </div>
  );
}
