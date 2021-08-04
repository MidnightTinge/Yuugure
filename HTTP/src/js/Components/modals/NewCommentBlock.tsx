import * as React from 'react';
import {useMemo, useState} from 'react';
import KY from '../../classes/KY';
import RouterResponseConsumer, {MESSAGES} from '../../classes/RouterResponseConsumer';
import Util from '../../classes/Util';
import Button from '../Button';
import Spinner from '../Spinner';

export type NewCommentBlockProps = {
  targetType: string;
  targetId: number;

  onCommentPosted?: (comment: CommentResponse) => void;
};

export const NewCommentBlock: React.FC<NewCommentBlockProps> = (props: NewCommentBlockProps) => {
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string>(null);

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
      console.warn('consumed:', consumed);
      if (consumed.success) {
        const [commentResponse] = consumed.data;
        txtComment.current.value = '';
        setError(null);

        if (typeof props.onCommentPosted === 'function') {
          props.onCommentPosted(commentResponse);
        }
      } else if (consumed.code === 429) {
        if (Array.isArray(consumed.data) && consumed.data.length > 0) {
          setError(`You are doing that too often. Try again in ${((((consumed.data[0] as unknown) as RateLimitResponse).minimum_wait) / 1e3).toFixed(3)}s`);
        } else {
          setError(MESSAGES[429]);
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
        {error ? (
          <p className="text-sm text-red-500 whitespace-pre-wrap">{error}</p>
        ) : null}
        <div className="mt-2 text-right">
          <Button type="submit" variant={!error ? 'green' : 'red'}>{posting ? (<Spinner/>) : 'Comment'}</Button>
        </div>
      </form>
    </div>
  );
};

export default NewCommentBlock;
