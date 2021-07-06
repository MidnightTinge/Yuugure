import * as React from 'react';
import {useContext, useEffect, useMemo, useReducer, useState} from 'react';
import {useParams} from 'react-router';
import namedContext from '../../classes/NamedContext';
import {XHR} from '../../classes/XHR';
import CenteredBlockPage from '../../Components/CenteredBlockPage';

import InternalNavContext from '../../Components/InternalNav/InternalNavContext';
import InternalRoute from '../../Components/InternalNav/InternalRoute';
import InternalRouter from '../../Components/InternalNav/InternalRouter';
import InternalSwitch from '../../Components/InternalNav/InternalSwitch';
import useInternalNavigator from '../../Components/InternalNav/useInternalNavigator';
import ListGroup from '../../Components/ListGroup/ListGroup';
import {CloseSource} from '../../Components/Modal/Modal';
import ReportModal from '../../Components/modals/ReportModal';
import Spinner from '../../Components/Spinner';
import UploadViewer from '../../Components/UploadViewer/UploadViewer';
import {useAuthState} from '../../Context/AuthStateProvider';
import {WebSocketContext} from '../../Context/WebSocketProvider';
import NotFound from '../404/NotFound';
import BookmarkFavBar, {Action, ActionHandler, BookmarkState} from './BookmarkFavBar/BookmarkFavBar';
import useCommentsReducer, {CommentsState} from './ViewerComments/CommentsReducer';
import ViewerComments from './ViewerComments/ViewerComments';
import ViewerDetails from './ViewerDetails';
import ViewerTags from './ViewerTags';

const UploadAction = Object.freeze({
  SET: 'set',
  PARTIAL: 'partial',

  UPLOAD_SET: 'set/upload',

  BOOKMARK_ADDED: 'bookmarks/total_added',
  BOOKMARK_REMOVED: 'bookmarks/total_removed',
  BOOKMARKS_SET: 'bookmarks/set',

  VOTE_SWAPPED: 'votes/swapped',
  VOTE_ADDED: 'votes/added',
  VOTE_REMOVED: 'votes/removed',
  VOTES_SET: 'votes/set',
});

type UploadState = {
  fetched: boolean;
  error: string;
  upload: RenderableUpload;
}

function UploadReducer(state: UploadState, action: ReducerAction): UploadState {
  switch (action.type) {
    case UploadAction.SET: {
      return {...action.payload};
    }
    case UploadAction.PARTIAL: {
      return {
        ...state,
        ...action.payload,
      };
    }
    case UploadAction.UPLOAD_SET: {
      return {
        ...state,
        upload: {...action.payload},
      };
    }

    case UploadAction.BOOKMARK_ADDED: {
      // someone added a bookmark

      if (state.upload) {
        const toSet = Object.assign({}, state);
        toSet.upload.bookmarks = Object.assign({}, toSet.upload.bookmarks, {
          total_public: toSet.upload.bookmarks.total_public + 1,
        });

        return toSet;
      }
      return state;
    }
    case UploadAction.BOOKMARK_REMOVED: {
      // someone removed a bookmark

      if (state.upload) {
        const toSet = Object.assign({}, state);
        toSet.upload.bookmarks = Object.assign({}, toSet.upload.bookmarks, {
          total_public: toSet.upload.bookmarks.total_public - 1,
        });

        return toSet;
      }
      return state;
    }
    case UploadAction.BOOKMARKS_SET: {
      if (state.upload) {
        const toSet = Object.assign({}, state);
        toSet.upload.bookmarks = Object.assign({}, toSet.upload.bookmarks, action.payload);

        return toSet;
      }
      return state;
    }

    case UploadAction.VOTE_SWAPPED: {
      // someone swapped one vote type for another (up->down/down->up)

      if (state.upload) {
        const toSet = Object.assign({}, state);
        toSet.upload.votes = Object.assign({}, toSet.upload.votes, {
          // subtract 1 from downvotes if we swapped to an upvote
          total_downvotes: toSet.upload.votes.total_downvotes + (action.payload.upvote ? -1 : 1),
          // add 1 to upvotes if we swapped to an upvote
          total_upvotes: toSet.upload.votes.total_upvotes + (action.payload.upvote ? 1 : -1),
        });

        return toSet;
      }
      return state;
    }
    case UploadAction.VOTE_ADDED: {
      // someone added a new vote

      if (state.upload) {
        const toSet = Object.assign({}, state);

        let toMerge: Partial<UploadVoteState> = (action.payload.upvote) ? ({total_upvotes: toSet.upload.votes.total_upvotes + 1}) : ({total_downvotes: toSet.upload.votes.total_downvotes + 1});
        toSet.upload.votes = Object.assign({}, toSet.upload.votes, toMerge);

        return toSet;
      }
      return state;
    }
    case UploadAction.VOTE_REMOVED: {
      // someone removed a vote

      if (state.upload) {
        const toSet = Object.assign({}, state);

        let toMerge: Partial<UploadVoteState> = (action.payload.upvote) ? ({total_upvotes: toSet.upload.votes.total_upvotes - 1}) : ({total_downvotes: toSet.upload.votes.total_downvotes - 1});
        toSet.upload.votes = Object.assign({}, toSet.upload.votes, toMerge);

        return toSet;
      }
      return state;
    }
    case UploadAction.VOTES_SET: {
      if (state.upload) {
        const toSet = Object.assign({}, state);
        toSet.upload.votes = Object.assign({}, toSet.upload.votes, action.payload);

        return toSet;
      }
      return state;
    }
  }

  return state;
}

export type PageViewProps = {
  //
};

type PageViewContextProps = {
  upload: Nullable<RenderableUpload>,
  comments: CommentsState,
}
export const PageViewContext = namedContext<PageViewContextProps>('PageViewContext', {upload: null, comments: {comments: [], fetching: false, error: null}});

export default function PageView(props: PageViewProps) {
  const params = useParams<{ uploadId: string }>();
  const authState = useAuthState();
  const navigator = useInternalNavigator(true);
  const [got404, setGot404] = useState(false);
  const [censored, setCensored] = useState(false);
  const [constrain, setConstrain] = useState(true);
  const [showReport, setShowReport] = useState(false);

  const [renderable, uploadDispatch] = useReducer(UploadReducer, {fetched: false, error: null, upload: null}, () => ({fetched: false, error: null, upload: null}));
  const {upload, fetched, error} = renderable;

  const [comments, commentsDispatch] = useCommentsReducer();

  const reportable = useMemo<{ type: string, id: number }>(() => (upload ? {type: 'upload', id: upload.upload.id} : {type: null, id: null}), [upload]);

  const {ws, rooms} = useContext(WebSocketContext);

  useEffect(function mounted() {
    function handleComment({comment}: { comment: RenderableComment }) {
      if (comment) {
        commentsDispatch({type: 'comments/add', payload: comment});
      }
    }

    function handleVote(packet: VotesUpdatedPacket) {
      switch (packet.action) {
        case 'add': {
          uploadDispatch({type: UploadAction.VOTE_ADDED, payload: {upvote: packet.upvote}});
          break;
        }
        case 'remove': {
          uploadDispatch({type: UploadAction.VOTE_REMOVED, payload: {upvote: packet.upvote}});
          break;
        }
        case 'swap': {
          uploadDispatch({type: UploadAction.VOTE_SWAPPED, payload: {upvote: packet.upvote}});
          break;
        }
      }
    }

    function handleBookmark(packet: BookmarksUpdatedPacket) {
      uploadDispatch({type: packet.change === 'add' ? UploadAction.BOOKMARK_ADDED : UploadAction.BOOKMARK_REMOVED});
    }

    if (params && params.uploadId) {
      if (ws != null) {
        ws.on('comment', handleComment);
        ws.on('votes_updated', handleVote);
        ws.on('bookmarks_updated', handleBookmark);
        rooms.join(`upload:${params.uploadId}`);
      }

      XHR.for(`/api/upload/${params.uploadId}`).get().getRouterResponse<RenderableUpload>().then(consumed => {
        if (consumed.success) {
          uploadDispatch({type: UploadAction.UPLOAD_SET, payload: consumed.data[0]});
        } else {
          setGot404(consumed.code === 404);
          uploadDispatch({type: UploadAction.PARTIAL, payload: {error: consumed.message}});
        }
      }).catch(err => {
        console.error('failed to get upload:', err);
        uploadDispatch({type: UploadAction.PARTIAL, payload: {error: err.toString()}});
      }).then(() => {
        uploadDispatch({type: UploadAction.PARTIAL, payload: {fetched: true}});
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
        ws.removeEventHandler('bookmarks_updated', handleBookmark);
        ws.removeEventHandler('votes_updated', handleVote);
      }
    };
  }, []);

  useEffect(() => {
    (window as any).CURRENT_UPLOAD = upload;
  }, [upload]);

  const bookmarkState: BookmarkState = useMemo(() => {
    let ret: BookmarkState = {
      bookmark: {
        active: false,
        isPrivate: false,
      },
      downvote: false,
      upvote: false,
    };

    if (upload != null) {
      ret = {
        bookmark: {
          active: upload.bookmarks.bookmarked,
          isPrivate: !upload.bookmarks.bookmarked_publicly,
        },
        upvote: upload.votes.voted && upload.votes.is_upvote,
        downvote: upload.votes.voted && !upload.votes.is_upvote,
      };
    }

    return ret;
  }, [upload?.bookmarks, upload?.votes]);

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

  function closeModal() {
    setShowReport(false);
  }

  function handleCloseRequest(cs: CloseSource, posting: boolean) {
    if (!posting) {
      closeModal();
    }
  }

  const handleBookmarkBarAction: ActionHandler = (action: Action) => {
    if (upload == null) return;

    let url = `/api/upload/${upload.upload.id}`;
    if (action.type === 'bookmark') {
      url += `/bookmark?private=${!action.args.public}`;
    } else if (action.type === 'vote') {
      url += `/${action.args.upvote ? 'upvote' : 'downvote'}`;
    }

    (action.args.remove ? XHR.for(url).delete(XHR.BODY_TYPE.NONE) : XHR.for(url).patch(XHR.BODY_TYPE.NONE))
      .getRouterResponse<boolean>()
      .then(consumed => {
        if (consumed.success) {
          let [updated] = consumed.data;
          if (updated) {
            // note: I'm having issues getting consistent results using spread, not sure if I'm
            //       doing something wrong or what but I'm moving forward with assign until I can
            //       figure out what exactly is wrong. My main concern right now is to get this
            //       functional.
            if (action.type === 'bookmark') {
              const bookmarks = Object.assign({}, upload.bookmarks, {
                bookmarked_publicly: action.args.public === true,
                bookmarked: !action.args.remove,
              });

              uploadDispatch({type: UploadAction.BOOKMARKS_SET, payload: bookmarks});
            } else if (action.type === 'vote') {
              const votes = Object.assign({}, upload.votes, {
                is_upvote: action.args.upvote === true,
                voted: !action.args.remove,
              });

              uploadDispatch({type: UploadAction.VOTES_SET, payload: votes});
            }
          }
        }
      })
      .catch(err => {
        console.error('Failed to update bookmark state:', err);
      });
  };

  return (
    <PageViewContext.Provider value={{upload, comments}}>
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
                  {/* the box-shadow on the internal navigation makes the spacing here a bit wonky */}
                  {authState.authed ? (
                    <section className="mt-1.5">
                      <BookmarkFavBar resourceType="upload" bookmarkState={bookmarkState} onAction={handleBookmarkBarAction} resourceId={upload.upload.id}/>
                    </section>
                  ) : null}
                  <section className="mt-1">
                    <ViewerDetails/>
                  </section>
                  <section className="mt-1">
                    <ViewerTags/>
                  </section>
                </div>
                <div className="col-span-8 md:col-span-10">
                  <InternalSwitch>
                    <InternalRoute path="comments">
                      <ViewerComments/>
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
    </PageViewContext.Provider>
  );
}
