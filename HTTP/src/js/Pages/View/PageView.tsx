import * as React from 'react';
import {useContext, useEffect, useMemo, useReducer, useState} from 'react';
import {useParams} from 'react-router';
import {useHistory} from 'react-router-dom';
import {AutoSizer, CellMeasurer, CellMeasurerCache, List, ListRowProps} from 'react-virtualized';
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
import {AuthStateContext} from '../../Context/AuthStateProvider';
import {WebSocketContext} from '../../Context/WebSocketProvider';
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
  const {state: authState} = useContext(AuthStateContext);
  const navigator = useInternalNavigator(true);
  const [fetched, setFetched] = useState(false);
  const [error, setError] = useState<string>(null);
  const [upload, setUpload] = useState<RenderableUpload>(null);
  const [got404, setGot404] = useState(false);
  const [censored, setCensored] = useState(false);
  const [constrain, setConstrain] = useState(true);
  const [showReport, setShowReport] = useState(false);

  const [comments, commentsDispatch] = useReducer(CommentsReducer, {comments: [], fetching: false, error: null}, () => ({comments: [], fetching: false, error: null}));

  const reportable = useMemo<{ type: string, id: number }>(() => (upload ? {type: 'upload', id: upload.upload.id} : {type: null, id: null}), [upload]);

  const {ws, rooms} = useContext(WebSocketContext);

  useEffect(function mounted() {
    function handleComment({comment}: { comment: RenderableComment }) {
      if (comment) {
        commentsDispatch({type: 'comments/add', payload: comment});
      }
    }

    if (params && params.uploadId) {
      if (ws != null) {
        ws.on('comment', handleComment);
        rooms.join(`upload:${params.uploadId}`);
      }

      XHR.for(`/api/upload/${params.uploadId}`).get().getRouterResponse<RenderableUpload>().then(consumed => {
        if (consumed.success) {
          setUpload(consumed.data[0]);
        } else {
          setError(consumed.message);
        }
      }).catch(err => {
        console.error('failed to get upload:', err);
        setError(err.toString());
      }).then(() => {
        setFetched(true);
      });

      commentsDispatch({type: 'fetch/fetching', payload: true});
      XHR.for(`/api/comment/upload/${params.uploadId}`).get().getRouterResponse<RenderableComment>().then(consumed => {
        if (consumed.success) {
          commentsDispatch({type: 'comments/set', payload: [...consumed.data]});
        } else {
          commentsDispatch({type: 'fetch/error', payload: consumed.message});
        }
      }).catch(err => {
        console.error('Failed to fetch comments.', err);
        commentsDispatch({type: 'fetch/error', payload: err});
      }).then(() => {
        commentsDispatch({type: 'fetch/fetching', payload: false});
      });
    }

    return function unmounted() {
      if (ws != null) { // ws shouldn't ever be null but just in case.
        rooms.leave(`upload:${params.uploadId}`);
        ws.removeEventHandler('comment', handleComment);
      }
    };
  }, []);

  useEffect(() => {
    (window as any).CURRENT_UPLOAD = upload;
  }, [upload]);

  const tags = useMemo(() => {
    if (upload == null) {
      return {user: [], system: []};
    }

    let user = [];
    let system = [];
    for (let tag of upload.tags) {
      if (tag.category === 'userland') {
        user.push(tag);
      } else {
        system.push(tag);
      }
    }

    user.sort((a, b) => a.name.localeCompare(b.name));
    system.sort((a, b) => a.name.localeCompare(b.name));

    return {user, system};
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

  function makeInternalNavigator(to: string) {
    return (e: React.MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      navigator.navigate(to);
    };
  }

  function makeRedirector(to: string) {
    return (e: React.MouseEvent<HTMLAnchorElement>) => {
      e.preventDefault();
      history.push(to);
    };
  }

  function closeModal() {
    setShowReport(false);
  }

  function handleCloseRequest(cs: CloseSource, posting: boolean) {
    if (!posting) {
      closeModal();
    }
  }

  const cellMeasurerCache = useMemo(() => {
    return new CellMeasurerCache({
      defaultHeight: 50,
      fixedWidth: true,
      keyMapper: (row) => {
        return comments.comments[row].id;
      },
    });
  }, [comments]);

  function commentRowRenderer({key, index, style, parent}: ListRowProps): React.ReactNode {
    return (
      <CellMeasurer
        cache={cellMeasurerCache}
        columnIndex={0}
        key={key}
        rowIndex={index}
        parent={parent}>
        {({registerChild}) => (
          <div ref={registerChild} style={style} className="px-2 py-0.5">
            <Comment comment={comments.comments[index]}/>
          </div>
        )}
      </CellMeasurer>
    );
  }

  return (
    <>
      <ReportModal targetType={reportable.type} targetId={reportable.id} onCloseRequest={handleCloseRequest} show={showReport}/>
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
            <div className="grid grid-cols-12 gap-2 p-2 pb-0 h-full">
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
                        <ListGroup.Item active={path === 'view'} onClick={makeInternalNavigator('view')}><i className="fas fa-image"/> View</ListGroup.Item>
                        <ListGroup.Item active={path === 'comments'} onClick={makeInternalNavigator('comments')}>
                          <i className="fas fa-comment-alt" aria-hidden={true}/> Comments
                          <span className="inline-block relative top-1 text-sm leading-none px-3 float-right rounded-lg bg-blue-100 border border-blue-200 text-blue-400 opacity-95 shadow">
                              {comments.fetching ? (<Spinner/>) : (comments.comments.length)}
                            </span>
                        </ListGroup.Item>
                        <ListGroup.Item active={path === 'edit'} onClick={makeInternalNavigator('edit')}><i className="fas fa-pencil-alt"/> Edit</ListGroup.Item>
                        <ListGroup.Item active={path === 'actions'} onClick={makeInternalNavigator('actions')}><i className="fas fa-wrench"/> Actions</ListGroup.Item>
                      </ListGroup>
                    )}
                  </InternalNavContext.Consumer>
                </section>
                <section className="mt-2">
                  <div className="rounded bg-gray-200 border border-gray-300 shadow-sm">
                    <div className="py-0.5 text-center text-gray-800 border-b border-gray-300">Tags</div>
                    <div className="p-2">
                      {tags.system.map(tag => (<div><a href={`/search?q=${encodeURIComponent(`${tag.category}:${tag.name}`)}`} onClick={makeRedirector(`/search?q=${encodeURIComponent(`${tag.category}:${tag.name}`)}`)} className="whitespace-pre-wrap break-all text-gray-700 hover:underline hover:text-gray-500" key={tag.id} data-tag={tag.id} data-category={tag.category} data-name={tag.name}>{tag.category}:{tag.name}</a></div>))}
                      {tags.user.map(tag => (<div><a href={`/search?q=${encodeURIComponent(tag.name)}`} onClick={makeRedirector(`/search?q=${encodeURIComponent(tag.name)}`)} className="whitespace-pre-wrap break-all text-gray-700 hover:underline hover:text-gray-500" key={tag.id} data-tag={tag.id} data-category={tag.category} data-name={tag.name}>{tag.name}</a></div>))}
                    </div>
                  </div>
                </section>
              </div>
              <div className="col-span-8 md:col-span-10">
                <InternalSwitch>
                  <InternalRoute path="comments">
                    <div className="flex flex-col h-full">
                      {authState.authed || comments.error ? (
                        <div className="bg-gray-100 border border-gray-200 rounded-sm shadow px-3 py-1 mb-2 flex-grow-0 flex-shrink">
                          {comments.error ? (<p className="text-red-500 text-lg whitespace-pre-wrap">{error}</p>) : null}
                          {authState.authed ? (
                            authState.account.state.COMMENTS_RESTRICTED ? (
                              <p className="text-center text-red-500 text-lg">You are restricted from creating new comments.</p>
                            ) : (
                              <NewCommentBlock targetType="upload" targetId={upload.upload.id}/>
                            )
                          ) : null}
                        </div>
                      ) : null}
                      <div className="flex-grow flex-shrink-0">
                        <AutoSizer>
                          {({width, height}) => (
                            <List
                              width={width}
                              height={height}
                              deferredMeasurementCache={cellMeasurerCache}
                              overscanRowCount={0}
                              rowCount={comments.comments.length}
                              rowHeight={({index}) => cellMeasurerCache.getHeight(index, 0)}
                              rowRenderer={commentRowRenderer}
                            />
                          )}
                        </AutoSizer>
                      </div>
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
