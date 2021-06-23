import * as React from 'react';
import {useEffect, useMemo, useReducer, useRef, useState} from 'react';
import {useParams} from 'react-router';
import {useHistory} from 'react-router-dom';
import WS from '../../classes/WS';
import {XHR} from '../../classes/XHR';
import CenteredBlockPage from '../../Components/CenteredBlockPage';
import Comment from '../../Components/Comment';

import InternalNavContext from '../../Components/InternalNav/InternalNavContext';
import InternalRoute from '../../Components/InternalNav/InternalRoute';
import InternalRouter from '../../Components/InternalNav/InternalRouter';
import InternalSwitch from '../../Components/InternalNav/InternalSwitch';
import useInternalNavigator from '../../Components/InternalNav/useInternalNavigator';
import ListGroup from '../../Components/ListGroup/ListGroup';
import {CloseSource} from '../../Components/Modal/Modal';
import NewCommentBlock from '../../Components/modals/NewCommentBlock';
import ReportModal from '../../Components/modals/ReportModal';
import Spinner from '../../Components/Spinner';
import UploadViewer from '../../Components/UploadViewer/UploadViewer';
import {authStateSelector} from '../../Stores/AuthStore';
import NotFound from '../404/NotFound';

type CommentsState = {
  fetching: boolean;
  error: string;
  comments: RenderableComment[];
}

function CommentsReducer(state: CommentsState, action: { type: string, payload?: any }): CommentsState {
  switch (action.type) {
    case 'fetching/fetching': {
      return {
        ...state,
        fetching: action.payload,
      };
    }
    case 'fetching/error': {
      return {
        ...state,
        error: action.payload,
      };
    }
    case 'fetching/set': {
      return {
        ...state,
        ...action.payload,
      };
    }
    case 'comments/add': {
      let payload = Array.isArray(action.payload) ? action.payload : [action.payload];
      return {
        ...state,
        comments: [...payload, ...state.comments],
      };
    }
    case 'comments/remove': {
      return {
        ...state,
        comments: state.comments.filter(x => x.id !== action.payload.id),
      };
    }
    case 'comments/update': {
      return {
        ...state,
        comments: state.comments.map(x => x.id === action.payload.id ? {...x, ...action.payload} : x),
      };
    }
    case 'comments/set': {
      let payload = Array.isArray(action.payload) ? action.payload : [action.payload];
      return {
        ...state,
        comments: [...payload],
      };
    }
  }

  return state;
}

export type PageViewProps = {
  //
};

export default function PageView(props: PageViewProps) {
  const params = useParams<{ uploadId: string }>();
  const history = useHistory();
  const authState = authStateSelector();
  const navigator = useInternalNavigator(true);
  const [fetched, setFetched] = useState(false);
  const [error, setError] = useState<string>(null);
  const [upload, setUpload] = useState<RenderableUpload>(null);
  const [got404, setGot404] = useState(false);
  const [censored, setCensored] = useState(false);
  const [constrain, setConstrain] = useState(true);
  const [showReport, setShowReport] = useState(false);

  const [comments, commentsDispatch] = useReducer(CommentsReducer, {comments: [], fetching: false, error: null}, () => ({comments: [], fetching: false, error: null}));

  const renderer = useRef<HTMLElement>(null);
  const ws = useRef<WS>(null);

  const reportable = useMemo<{ type: string, id: number }>(() => (upload ? {type: 'upload', id: upload.upload.id} : {type: null, id: null}), [upload]);

  useEffect(function mounted() {
    if (params && params.uploadId) {
      // TODO listen to WS for upload-specific events (new comment, new rating, etc)
      // ws.current = new WS();
      // ws.current.connect();
      // ws.current.emit('join', `upload-${params.uploadId}`);

      XHR.for(`/api/upload/${params.uploadId}`).get().getJson<RouterResponse<RenderableUpload>>().then(res => {
        if (res.code === 401) {
          setError('You must be logged in to view this resource.');
        } else if (res.code === 403) {
          setError('You do not have permission to view this resource.');
        } else if (res.code === 404) {
          setGot404(true);
        } else if (res.code === 200 && res.data && Array.isArray(res.data.RenderableUpload) && res.data.RenderableUpload.length > 0) {
          setUpload(res.data.RenderableUpload[0]);
        } else {
          setError('An internal server error occurred.');
        }
      }).catch(err => {
        console.error('failed to get upload:', err);
        setError(err.toString());
      }).then(() => {
        setFetched(true);
      });

      commentsDispatch({type: 'fetch/fetching', payload: true});
      XHR.for(`/api/comment/upload/${params.uploadId}`).get().getJson<RouterResponse<RenderableComment>>().then(res => {
        if (res) {
          if (res.code === 200 && Array.isArray(res.data.RenderableComment)) {
            commentsDispatch({type: 'comments/set', payload: [...res.data.RenderableComment]});
          } else {
            commentsDispatch({type: 'fetch/error', payload: 'Received an invalid response. Please try again later.'});
          }
        } else {
          commentsDispatch({type: 'fetch/error', payload: 'An internal server error occurred. Please try again later.'});
        }
      }).catch(err => {
        console.error('Failed to fetch comments.', err);
        commentsDispatch({type: 'fetch/error', payload: err});
      }).then(() => {
        commentsDispatch({type: 'fetch/fetching', payload: false});
      });
    }

    return function unmounted() {
      if (ws.current != null) { // ws is null if our request param was invalid (no upload to fetch)
        ws.current.disconnect();
      }
    };
  }, []);

  useEffect(() => {
    (window as any).CURRENT_UPLOAD = upload;
  }, [upload]);

  function handleReport() {
    setShowReport(true);
  }

  function handleDelete() {
    console.warn('Need to delete (not yet implemented)');
  }

  function handleToggleResize() {
    setConstrain(!constrain);
  }

  function makeNavigator(to: string) {
    return (e: React.MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      navigator.navigate(to);
    };
  }

  function closeModal() {
    setShowReport(false);
  }

  function handleReportSent() {
    //
  }

  function handleCloseRequest(cs: CloseSource, posting: boolean) {
    if (!posting) {
      closeModal();
    }
  }

  function handleCommentPosted(response: CommentResponse) {
    console.debug('[handleCommentPosted]', response);
    if (response && response.comment) {
      commentsDispatch({type: 'comments/add', payload: response.comment});
    }
  }

  return (
    <>
      <ReportModal targetType={reportable.type} targetId={reportable.id} onReportSent={handleReportSent} onCloseRequest={handleCloseRequest} show={showReport}/>
      {!fetched ? (
        <div className="flex flex-col w-full h-full items-center justify-center">
          <div className="flex-shrink">
            <div className="block text-center">
              <i className="fas fa-circle-notch fa-spin fa-4x text-gray-300"/>
            </div>
          </div>
        </div>
      ) : (
        (error || !upload ? (
          got404 ? (
            <NotFound/>
          ) : (
            <CenteredBlockPage pageBackground="bg-red-50" cardBackground="bg-red-200" cardBorder="border-red-300">
              <p className="text-center">Failed to request the upload from the server. Please try again.</p>
              <p className="text-sm mt-4">Error:</p>
              <p className="block ml-2 text-sm whitespace-pre-wrap text-gray-800">{error || 'An internal server error occurred. No upload was returned by the server.'}</p>
            </CenteredBlockPage>
          )
        ) : (
          <InternalRouter defaultPath="view">
            <div className="grid grid-cols-12 gap-2 p-2">
              <div className="col-span-4 md:col-span-2">
                <section className="text-right">
                  {authState && authState.authed ? (
                    <div>
                      {authState.account.id === upload.owner.id ? (
                        <button onClick={handleDelete} className="text-sm text-blue-300 underline hover:text-blue-400 focus:outline-none">Delete</button>
                      ) : null}
                      <button onClick={handleReport} className="ml-1 text-sm text-blue-300 underline hover:text-blue-400 focus:outline-none">Report</button>
                    </div>
                  ) : null}
                  <div>
                    <a href={`/full/${upload.upload.id}`} target="_blank" className="underline text-sm text-blue-300 hover:text-blue-400 focus:outline-none">Direct Link</a>
                    <button className="ml-1 underline text-sm text-blue-300 hover:text-blue-400 focus:outline-none" onClick={handleToggleResize}>Toggle Resize</button>
                  </div>
                </section>
                <section className="mt-2">
                  <InternalNavContext.Consumer>
                    {({path = ''}) => (
                      <ListGroup>
                        <ListGroup.Item active={path === 'view'} onClick={makeNavigator('view')}><i className="fas fa-image"/> View</ListGroup.Item>
                        <ListGroup.Item active={path === 'comments'} onClick={makeNavigator('comments')}>
                          <i className="fas fa-comment-alt" aria-hidden={true}/> Comments
                          <span className="inline-block relative top-1 text-sm leading-none px-3 float-right rounded-lg bg-blue-100 border border-blue-200 text-blue-400 opacity-95 shadow">
                              {comments.fetching ? (<Spinner/>) : (comments.comments.length)}
                            </span>
                        </ListGroup.Item>
                        <ListGroup.Item active={path === 'edit'} onClick={makeNavigator('edit')}><i className="fas fa-pencil-alt"/> Edit</ListGroup.Item>
                        <ListGroup.Item active={path === 'actions'} onClick={makeNavigator('actions')}><i className="fas fa-wrench"/> Actions</ListGroup.Item>
                      </ListGroup>
                    )}
                  </InternalNavContext.Consumer>
                </section>
                <section className="mt-2">
                  <div className="rounded bg-gray-200 border border-gray-300 shadow-sm">
                    <div className="py-0.5 text-center text-gray-700 border-b border-gray-300">Tags</div>
                    <div className="p-2">
                      <p className="text-gray-400 italic text-sm text-center select-none">Placeholder</p>
                    </div>
                  </div>
                </section>
              </div>
              <div className="col-span-8 md:col-span-10">
                <InternalSwitch>
                  <InternalRoute path="comments">
                    {comments.comments.map((comment, idx) => (<Comment key={idx} comment={comment}/>))}
                    <div className="bg-gray-100 border border-gray-200 rounded-sm shadow p-3">
                      {authState.authed ? (
                        authState.account.state.COMMENTS_RESTRICTED ? (
                          <p className="text-center text-red-500 text-lg">You are restricted from creating new comments.</p>
                        ) : (
                          <NewCommentBlock targetType="upload" targetId={upload.upload.id} onCommentPosted={handleCommentPosted}/>
                        )
                      ) : null}
                    </div>
                  </InternalRoute>
                  <InternalRoute path="edit">
                    <p>hello edit</p>
                  </InternalRoute>
                  <InternalRoute path="actions">
                    <p>hello actions</p>
                  </InternalRoute>
                  <InternalRoute path="*">
                    <UploadViewer upload={upload.upload} media={upload.media} censored={censored} constrained={constrain}/>
                  </InternalRoute>
                </InternalSwitch>
              </div>
            </div>
          </InternalRouter>
        ))
      )}
    </>
  );
}
