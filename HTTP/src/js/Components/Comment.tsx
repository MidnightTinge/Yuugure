import {mdiAlertOctagon} from '@mdi/js';
import Icon from '@mdi/react';
import anchorme from 'anchorme';
import * as React from 'react';
import {useContext, useMemo, useState} from 'react';
import RelativeTime from '../classes/RelativeTime';
import {AuthStateContext} from '../Context/AuthStateProvider';
import Button from './Button';
import {CloseSource} from './Modal/Modal';
import ReportModal from './modals/ReportModal';

export type CommentProps = {
  comment: RenderableComment;
};

export const Comment: React.FC<CommentProps> = ({comment}: CommentProps) => {
  const [showReport, setShowReport] = useState(false);
  const {state: authState} = useContext(AuthStateContext);

  const parsed = useMemo(() => {
    return anchorme({
      input: comment.content_rendered,
      options: {
        protocol: 'https://',
        specialTransform: [
          {
            test: /.*/,
            transform: (input) => {
              let url = input;
              if (!/https?:\/\//i.test(input)) url = `https://${input}`;

              return `<a href="/leaving?url=${encodeURIComponent(url)}" class="text-blue-500 hover:text-blue-700 underline" target="_blank" rel="nofollow noopener noreferrer">${input}</a>`;
            },
          },
        ],
      },
    });
  }, [comment.content_rendered]);

  function onReportClicked() {
    setShowReport(true);
  }

  function onReportSent() {
    //
  }

  function onCloseRequest(cs: CloseSource, posting: boolean) {
    if (!posting) {
      setShowReport(false);
    }
  }

  return (
    <>
      {authState.authed ? <ReportModal show={showReport} targetType="comment" targetId={comment.id} onReportSent={onReportSent} onCloseRequest={onCloseRequest}/> : null}
      <div data-comment={comment.id} data-account={comment.account.id} id={`comment-${comment.id}`} className="bg-gray-100 border border-gray-200 rounded-sm shadow flex flex-col mb-2">
        <div className="flex-shrink flex-grow-0 flex flex-row bg-gray-200 px-1">
          <div className="flex-grow">
            <a href={`/user/${comment.account.id}`} target="_blank" className="text-gray-700 font-medium hover:underline hover:text-gray-800">{comment.account.username}</a>
          </div>
          <div className="flex-shrink">
            {authState.authed ? (<Button variant="gray" title="Report" className="mr-2" onClick={onReportClicked} link><Icon path={mdiAlertOctagon} size={1} className="relative bottom-px inline-block"/><span className="sr-only">Report</span></Button>) : null}
            <a href={`#comment-${comment.id}`} className="text-xs text-gray-400 hover:text-gray-500 underline" title={new Date(comment.timestamp).toString()}>{RelativeTime(comment.timestamp)}</a>
          </div>
        </div>
        <div className="py-1 px-1">
          <p className="whitespace-pre-wrap" dangerouslySetInnerHTML={{__html: parsed || comment.content_rendered || comment.content_raw}}/>
        </div>
      </div>
    </>
  );
};

export default Comment;
