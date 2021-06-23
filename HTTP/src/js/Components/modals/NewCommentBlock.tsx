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
  const [state, setState] = useState<{ posting: boolean, error: string, posted: boolean }>({posting: false, error: null, posted: false});
  const [commentValidity, setCommentValidity] = useState<InputValidity>({valid: true, error: null});

  const txtComment = React.useRef<HTMLTextAreaElement>();
  const id = useMemo(() => Util.mkid(), []);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setState({
      ...state,
      posting: true,
      posted: false,
    });

    XHR.for(`/api/comment/${props.targetType}/${props.targetId}`).post(XHR.BODY_TYPE.FORM, {
      body: txtComment.current.value,
    }).getJson<RouterResponse<CommentResponse>>().then(res => {
      if (res) {
        if (res.code === 200 && Array.isArray(res.data.CommentResponse) && res.data.CommentResponse[0]) {
          txtComment.current.value = '';

          setState({
            ...state,
            error: null,
            posted: true,
          });

          if (typeof props.onCommentPosted === 'function') {
            props.onCommentPosted(res.data.CommentResponse[0]);
          }
        }
      } else {
        setState({
          ...state,
          error: 'An internal server error occurred. Please try again later.',
          posted: false,
        });
        console.error('Received invalid CommentResponse:', res);
      }
    }).catch(err => {
      setState({
        ...state,
        error: err.toString(),
      });
    }).then(() => {
      setState({
        ...state,
        posting: false,
      });
    });

  }

  return (
    <div>
      <form method="POST" action={`/api/${props.targetType}/${props.targetId}`} onSubmit={handleSubmit}>
        <label htmlFor={id} className="text-gray-500">Comment:</label>
        <textarea ref={txtComment} id={id} name="body" className="block w-full rounded-md shadow border bg-gray-50 border-gray-300 hover:bg-gray-100 disabled:cursor-not-allowed disabled:bg-gray-200" disabled={state.posting} required/>
        <div className="mt-2 text-right">
          <button type="submit" className="px-2 py-1 bg-green-500 border border-green-700 hover:bg-green-700 text-white rounded-md shadow-sm font-medium">{state.posting ? (<Spinner/>) : 'Comment'}</button>
        </div>
      </form>
    </div>
  );
}
