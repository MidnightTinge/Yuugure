import * as React from 'react';
import {useContext, useEffect, useState} from 'react';
import {useParams} from 'react-router';
import KY from '../../classes/KY';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';

import {useAlerts} from '../../Components/Alerts/AlertsProvider';
import CenteredBlockPage from '../../Components/CenteredBlockPage';
import InternalNavContext from '../../Components/InternalNav/InternalNavContext';
import InternalRoute from '../../Components/InternalNav/InternalRoute';
import InternalRouter from '../../Components/InternalNav/InternalRouter';
import InternalSwitch from '../../Components/InternalNav/InternalSwitch';
import useInternalNavigator from '../../Components/InternalNav/useInternalNavigator';
import ListGroup from '../../Components/ListGroup/ListGroup';
import LoadingPing from '../../Components/LoadingPing';
import {CloseSource} from '../../Components/Modal/Modal';
import ReportModal from '../../Components/modals/ReportModal';
import Spinner from '../../Components/Spinner';
import {AuthStateContext} from '../../Context/AuthStateProvider';
import {WebSocketContext} from '../../Context/WebSocketProvider';
import useUploadReducer from '../../Hooks/useUploadReducer';
import NotFound from '../404/NotFound';
import AccountSettings from './AccountSettings';
import BulkPaginatedRenderer from './BulkPaginatedRenderer';

export type PageProfileProps = {
  self?: boolean;
};

export default function PageProfile(props: PageProfileProps) {
  const {ws, rooms} = useContext(WebSocketContext);

  const {state: authState} = useContext(AuthStateContext);
  const params = useParams<{ accountId: string }>();
  const navigator = useInternalNavigator(true);

  const alerts = useAlerts();

  const [fetching, setFetching] = useState(false);
  const [fetched, setFetched] = useState(false);
  const [error, setError] = useState<string>(null);
  const [got404, setGot404] = useState(false);

  const [fetchingUploads, setFetchingUploads] = useState(false);
  const [uploadsErrored, setUploadsErrored] = useState(false);

  const [fetchingBookmarks, setFetchingBookmarks] = useState(false);
  const [bookmarksErrored, setBookmarksErrored] = useState(false);

  const [profile, setProfile] = useState<ProfileResponse>(null);

  const uploadsReducer = useUploadReducer();
  const bookmarksReducer = useUploadReducer();
  const [/* not used */, /* not used */, uploadActions] = uploadsReducer;

  const [totalUploads, setTotalUploads] = useState<number>(null);
  const [totalBookmarks, setTotalBookmarks] = useState<number>(null);

  const [showReport, setShowReport] = useState(false);

  const [lastAccountId, setLastAccountId] = useState<string>(null);

  const accountId = params.accountId;

  // note: It's possible for our authState to not be initialized yet, in which case this can flag
  //       as `false` incorrectly. the back-end corrects for this automatically on the profile API
  //       request.
  const isSelf = props.self || (authState.authed && params.accountId && Number(params.accountId) === authState.account.id);


  useEffect(function sub() {
    if (accountId === '@me' && !authState.authed) {
      // can't do anything here, we need authState.account.id to subscribe to the correct room.
      // we'll have to wait until authState reloads properly.
      return;
    }

    function handleUpload(args: { upload: RenderableUpload }) {
      uploadActions.add(args.upload);
    }

    function handleRemoveUpload({id}: RemoveUploadPacket) {
      uploadActions.remove(id);
    }

    let id = accountId === '@me' ? (authState.authed ? authState.account.id : null) : accountId;
    if (id !== null && ws != null) {
      ws.addEventHandler('upload', handleUpload);
      ws.addEventHandler('remove_upload', handleRemoveUpload);
      rooms.join(`account:${id}`);
    }

    return function unsub() {
      if (id !== null && ws != null) {
        ws.removeEventHandler('upload', handleUpload);
        ws.removeEventHandler('remove_upload', handleRemoveUpload);
        rooms.leave(`account:${id}`);
      }
    };
  }, [accountId, authState]);

  useEffect(function mounted() {
    setFetching(true);
    setFetched(false);

    KY.get(`/api/profile/${accountId}`).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<ProfileResponse>(data);
      if (consumed.success) {
        setProfile(consumed.data[0]);
      } else {
        setError(consumed.message);
      }
    }).catch(err => {
      console.error('Failed to fetch profile.', err);
      setError(err.toString());
    }).then(() => {
      setFetched(true);
      setFetching(false);
    });

    return function unmounted() {
      setFetching(false);
      setFetched(false);
      setGot404(false);
      setError(null);
      setFetchingUploads(false);
      setUploadsErrored(null);
      setShowReport(false);
    };
  }, [accountId]);

  useEffect(function profileEffect() {
    if (accountId === lastAccountId) return;
    if (profile == null) return;
    setLastAccountId(accountId);
  }, [profile?.account]);

  function makeNavigator(to: string) {
    return (e: React.MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      navigator.navigate(to);
    };
  }

  function onCloseRequest(cs: CloseSource, posting: boolean) {
    if (!posting) {
      setShowReport(false);
    }
  }

  function onReportSent() {
    // nothing to do
  }

  function handleReportClick() {
    setShowReport(true);
  }

  return (
    <>
      <ReportModal targetType="account" targetId={fetched && profile ? profile.account.id : null} onReportSent={onReportSent} onCloseRequest={onCloseRequest} show={showReport}/>
      {!error ? (
        got404 || ((!isSelf && !params.accountId) || (fetched && !profile)) ? (
          <NotFound/>
        ) : (
          fetching ? (
            <div className="flex flex-col items-center justify-center h-full">
              <LoadingPing icon="fas fa-address-card"/>
            </div>
          ) : (
            <InternalRouter defaultPath="details">
              <div className="grid grid-cols-12 gap-2 pt-2 pl-2 h-full max-w-screen overflow-x-hidden">
                <div className="col-span-4 md:col-span-2">
                  <section>
                    <InternalNavContext.Consumer>
                      {({path = ''}) => (
                        <ListGroup>
                          <ListGroup.Item active={path === 'details'} onClick={makeNavigator('details')}><i className="fas fa-address-card" aria-hidden={true}/> Details</ListGroup.Item>
                          <ListGroup.Item active={path === 'uploads'} onClick={makeNavigator('uploads')}>
                            <i className="fas fa-folder-open" aria-hidden={true}/> Uploads
                            {fetchingUploads || uploadsErrored ? (
                              <span className={`inline-block relative top-1 text-sm leading-none px-3 float-right rounded-lg border ${!uploadsErrored ? 'bg-blue-100 border-blue-200 text-blue-400' : 'bg-red-100 border-red-200 text-red-400'} opacity-95 shadow`}>
                                {fetchingUploads ? (<Spinner/>) : (
                                  uploadsErrored ? (<i className="fas fa-exclamation-triangle text-xs"/>) : null
                                )}
                              </span>
                            ) : null}
                          </ListGroup.Item>
                          <ListGroup.Item active={path === 'bookmarks'} onClick={makeNavigator('bookmarks')}>
                            <i className="fas fa-bookmark" aria-hidden={true}/> Bookmarks
                            {fetchingBookmarks || bookmarksErrored ? (
                              <span className={`inline-block relative top-1 text-sm leading-none px-3 float-right rounded-lg border ${!bookmarksErrored ? 'bg-blue-100 border-blue-200 text-blue-400' : 'bg-red-100 border-red-200 text-red-400'} opacity-95 shadow`}>
                                {fetchingBookmarks ? (<Spinner/>) : (
                                  bookmarksErrored ? (<i className="fas fa-exclamation-triangle text-xs"/>) : null
                                )}
                              </span>
                            ) : null}
                          </ListGroup.Item>
                          <ListGroup.Item active={path === 'votes'} onClick={makeNavigator('votes')}><i className="fas fa-check-circle" aria-hidden={true}/> Votes</ListGroup.Item>
                          {profile && profile.self ? (
                            <ListGroup.Item active={path === 'settings'} onClick={makeNavigator('settings')}><i className="fas fa-user-cog" aria-hidden={true}/> Settings</ListGroup.Item>
                          ) : null}
                        </ListGroup>
                      )}
                    </InternalNavContext.Consumer>
                  </section>
                </div>
                <div className="col-span-8 md:col-span-10">
                  <InternalSwitch>
                    <InternalRoute path="uploads">
                      <BulkPaginatedRenderer key="uploads" endpoint={`/api/profile/${accountId}/uploads`} reducer={uploadsReducer} setMax={setTotalUploads} onFetchState={fetching => setFetchingUploads(fetching)} onError={err => setUploadsErrored(!!err)}/>
                    </InternalRoute>
                    <InternalRoute path="bookmarks">
                      <BulkPaginatedRenderer key="bookmarks" endpoint={`/api/profile/${accountId}/bookmarks`} reducer={bookmarksReducer} setMax={setTotalBookmarks} onFetchState={fetching => setFetchingBookmarks(fetching)} onError={err => setBookmarksErrored(!!err)}/>
                    </InternalRoute>
                    <InternalRoute path="votes">
                      <p>hello votes</p>
                    </InternalRoute>
                    <InternalRoute path="settings">
                      <AccountSettings account={authState.account}/>
                    </InternalRoute>
                    <InternalRoute path="*">
                      {profile && profile.account ? (
                        <div className="py-3">
                          <p className="text-center text-lg">{profile.account.username}</p>
                          <hr/>
                          <table className="block">
                            <tbody>
                              <tr>
                                <th className="text-right pr-2">ID</th>
                                <td className="text-left">{profile.account.id}</td>
                              </tr>
                              {totalUploads != null ? (
                                <tr>
                                  <th className="text-right pr-2">Uploads</th>
                                  <td className="text-left">{totalUploads}</td>
                                </tr>
                              ) : null}
                              {totalBookmarks != null ? (
                                <tr>
                                  <th className="text-right pr-2">Bookmarks</th>
                                  <td className="text-left">{totalBookmarks}</td>
                                </tr>
                              ) : null}
                            </tbody>
                          </table>
                          <button className="underline text-blue-400 hover:text-blue-500 focus:outline-none" onClick={handleReportClick}>Report</button>
                        </div>
                      ) : null}
                    </InternalRoute>
                  </InternalSwitch>
                </div>
              </div>
            </InternalRouter>
          )
        )
      ) : (
        <CenteredBlockPage pageBackground="bg-red-50" cardBackground="bg-red-100" cardBorder="bg-red-200">
          <p className="text-center">An error occurred:</p>
          <p className="text-red whitespace-pre-wrap">{error}</p>
        </CenteredBlockPage>
      )}
    </>
  );
}
