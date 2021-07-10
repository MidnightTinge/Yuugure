import KY from '../../classes/KY';
import * as React from 'react';
import {useMemo, useState} from 'react';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Util from '../../classes/Util';
import Spinner from '../Spinner';

export type NewCommentBlockProps = {
  targetType: string;
  targetId: number;

  onCommentPosted?: (comment: CommentResponse) => void;
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
    KY.post(`/api/comment/${props.targetType}/${props.targetId}`, {
      body: Util.formatUrlEncodedBody({
        body: txtComment.current.value,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<CommentResponse>(data);
      if (consumed.success) {
        let [commentResponse] = consumed.data;
        txtComment.current.value = '';
        setError(null);

        if (typeof props.onCommentPosted === 'function') {
          props.onCommentPosted(commentResponse);
        }
      } else {
        setError(consumed.message);
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
