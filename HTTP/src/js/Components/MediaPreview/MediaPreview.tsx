import {mdiAlert, mdiBookmark, mdiBookmarkOutline, mdiChevronDownCircle, mdiChevronDownCircleOutline, mdiChevronUpCircle, mdiChevronUpCircleOutline, mdiEyeOff, mdiImage, mdiLock, mdiVideo} from '@mdi/js';
import Icon from '@mdi/react';
import clsx from 'clsx';
import * as React from 'react';
import {useHistory} from 'react-router-dom';


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
          <img src={`/thumb/${upload.upload.id}`} alt={`Thumbnail for upload ${upload.upload.id}`} className={clsx('thumbnail', upload.state.MODERATION_QUEUED && 'censored hover-clear')}/>
        </a>
        <div className="IconsWrapper">
          <div className="OverlayIcon" title={`This upload is a${isImage ? 'n image' : 'video'}`}>
            <Icon path={isImage ? mdiImage : mdiVideo} size={1}/>
          </div>
          {upload.state.PRIVATE ? (
            <div className="OverlayIcon" title="This upload is private.">
              <Icon path={mdiEyeOff} size={1}/>
            </div>
          ) : null}
          {upload.state.MODERATION_QUEUED ? (
            <div className="OverlayIcon" title="This upload is awaiting moderator approval.">
              <Icon path={mdiAlert} size={1}/>
            </div>
          ) : null}
        </div>
      </div>
      <div className="DetailsWrapper">
        <div className={clsx('Detail', upload.bookmarks.bookmarked && 'text-pink-500')} title="Public Favorites" data-detail="favorites" data-detail-value={upload.bookmarks.total_public}>
          <Icon path={upload.bookmarks.bookmarked ? mdiBookmark : mdiBookmarkOutline} size={1} className="inline-block mr-px relative bottom-px"/>
          <span className="text-pink-500">{upload.bookmarks.total_public}</span>
        </div>
        <div className={clsx('Detail', (upload.votes.voted && upload.votes.is_upvote) && 'text-green-500')} title="Upvotes" data-detail="downvotes" data-detail-value={upload.votes.total_upvotes}>
          <Icon path={(upload.votes.voted && upload.votes.is_upvote) ? mdiChevronUpCircle : mdiChevronUpCircleOutline} size={1} className="inline-block mr-px relative bottom-px"/>
          <span className="text-green-500">{upload.votes.total_upvotes}</span>
        </div>
        <div className={clsx('Detail', (upload.votes.voted && !upload.votes.is_upvote) && 'text-red-500')} title="Downvotes" data-detail="downvotes" data-detail-value={upload.votes.total_downvotes}>
          <Icon path={(upload.votes.voted && !upload.votes.is_upvote) ? mdiChevronDownCircle : mdiChevronDownCircleOutline} size={1} className="inline-block mr-px relative bottom-px"/>
          <span className="text-red-500">{upload.votes.total_downvotes}</span>
        </div>
      </div>
    </div>
  );
}
