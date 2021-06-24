import * as React from 'react';
import {useMemo, useState} from 'react';
import Util from '../../classes/Util';
import {XHR} from '../../classes/XHR';
import Spinner from '../Spinner';

export type NewCommentBlockProps = {
  targetType: string;
  targetId: number;

  onCommentPosted: (comment: CommentResponse) => void;
};

export default function NewCommentBlock(props: NewCommentBlockProps) {
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string>(null);
  const [commentValidity, setCommentValidity] = useState<InputValidity>({valid: true, error: null});

  const txtComment = React.useRef<HTMLTextAreaElement>();
  const id = useMemo(() => Util.mkid(), []);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setPosting(true);
    XHR.for(`/api/comment/${props.targetType}/${props.targetId}`).post(XHR.BODY_TYPE.FORM, {
      body: txtComment.current.value,
    }).getJson<RouterResponse<CommentResponse>>().then(res => {
      console.debug('res', res);
      if (res) {
        if (res.code === 200 && Array.isArray(res.data.CommentResponse) && res.data.CommentResponse[0]) {
          txtComment.current.value = '';
          setError(null);

          if (typeof props.onCommentPosted === 'function') {
            props.onCommentPosted(res.data.CommentResponse[0]);
          }
        } else if (res.code === 429) {
          setError(`You are doing that too often. Try again ${res.data && res.data.RateLimitResponse ? (`in ${res.data.RateLimitResponse[0].minimum_wait / 1e3 >> 0} seconds`) : 'later'}.`);
        }
      } else {
        setError('An internal server error occurred. Please try again later.');
        console.error('Received invalid CommentResponse:', res);
      }
    }).catch(err => {
      setError(err.toString());
    }).then(() => {
      setPosting(false);
    });
  }

  return (
    <div>
      <form method="POST" action={`/api/${props.targetType}/${props.targetId}`} onSubmit={handleSubmit}>
        <label htmlFor={id} className="text-gray-500">Comment:</label>
        <textarea ref={txtComment} id={id} name="body" className={`block w-full rounded-md shadow border ${!error ? 'bg-gray-50 border-gray-300 hover:bg-gray-100' : 'bg-red-50 border-red-300 hover:bg-red-100'} disabled:cursor-not-allowed disabled:bg-gray-200`} disabled={posting} required/>
        <div className="mt-2 text-right">
          <button type="submit" className={`px-2 py-1 ${!error ? 'bg-green-500 border border-green-700 hover:bg-green-700' : 'bg-red-500 border borer-red-700 hover:bg-red-700'} text-white rounded-md shadow-sm font-medium`}>{posting ? (<Spinner/>) : 'Comment'}</button>
        </div>
        {error ? (
          <p className="text-sm text-red-500 whitespace-pre-wrap">{error}</p>
        ) : null}
      </form>
    </div>
  );
}
