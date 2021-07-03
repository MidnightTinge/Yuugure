import * as React from 'react';
import {useContext, useEffect, useMemo, useState} from 'react';
import {useParams} from 'react-router';

import {AutoSizer, List, ListRowProps, Size} from 'react-virtualized';
import Util from '../../classes/Util';
import {XHR} from '../../classes/XHR';
import {AlertType} from '../../Components/Alerts/Alert/Alert';
import {useAlertContext} from '../../Components/Alerts/AlertsProvider';
import CenteredBlockPage from '../../Components/CenteredBlockPage';
import InternalNavContext from '../../Components/InternalNav/InternalNavContext';
import InternalRoute from '../../Components/InternalNav/InternalRoute';
import InternalRouter from '../../Components/InternalNav/InternalRouter';
import InternalSwitch from '../../Components/InternalNav/InternalSwitch';
import useInternalNavigator from '../../Components/InternalNav/useInternalNavigator';
import ListGroup from '../../Components/ListGroup/ListGroup';
import LoadingPing from '../../Components/LoadingPing';
import MediaPreviewBlock from '../../Components/MediaPreview/MediaPreviewBlock';
import {CloseSource} from '../../Components/Modal/Modal';
import ReportModal from '../../Components/modals/ReportModal';
import Spinner from '../../Components/Spinner';
import {AuthStateContext} from '../../Context/AuthStateProvider';
import {WebSocketContext} from '../../Context/WebSocketProvider';
import useUploadReducer from '../../Hooks/useUploadReducer';
import NotFound from '../404/NotFound';
import AccountSettings from './AccountSettings';

export type PageProfileProps = {
  self?: boolean;
};

export default function PageProfile(props: PageProfileProps) {
  const {ws, rooms} = useContext(WebSocketContext);

  const {state: authState} = useContext(AuthStateContext);
  const params = useParams<{ accountId: string }>();
  const navigator = useInternalNavigator(true);

  const alerts = useAlertContext();

  const [fetching, setFetching] = useState(false);
  const [fetched, setFetched] = useState(false);
  const [error, setError] = useState<string>(null);
  const [got404, setGot404] = useState(false);

  const [fetchingUploads, setFetchingUploads] = useState(false);
  const [uploadsErrored, setUploadsErrored] = useState<boolean>(false);

  const [profile, setProfile] = useState<ProfileResponse>(null);
  const [uploads, uploadsDispatch, uploadActions] = useUploadReducer();

  const [showReport, setShowReport] = useState(false);

  const [lastAccountId, setLastAccountId] = useState<string>(null);

  const accountId = useMemo(() => (props.self || (authState.authed && params.accountId && Number(params.accountId) === authState.account.id)) ? '@me' : params.accountId, [params, authState]);

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

    let id = accountId === '@me' ? (authState.authed ? authState.account.id : null) : accountId;
    if (id !== null && ws != null) {
      ws.addEventHandler('upload', handleUpload);
      rooms.join(`account:${id}`);
    }

    return function unsub() {
      if (id !== null && ws != null) {
        ws.removeEventHandler('upload', handleUpload);
        rooms.leave(`account:${id}`);
      }
    };
  }, [accountId, authState]);

  useEffect(function mounted() {
    setFetching(true);
    setFetched(false);

    XHR.for(`/api/profile/${accountId}`).get().getRouterResponse<ProfileResponse>().then(consumed => {
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

  useEffect(() => {
    if (accountId === lastAccountId) return;
    setLastAccountId(accountId);

    uploadActions.set([]);
    setFetchingUploads(true);
    XHR.for(`/api/profile/${accountId}/uploads`).get().getRouterResponse<BulkRenderableUpload>().then(consumed => {
      if (consumed.success) {
        uploadActions.set([...Util.mapBulkUploads(consumed.data[0])]);
        setUploadsErrored(false);
      } else {
        setUploadsErrored(true);
        alerts.add({
          type: AlertType.ERROR,
          header: (<><i className="fas fa-exclamation-triangle text-red-500" aria-hidden={true}/> Error</>),
          body: `Failed to fetch uploads.\n${consumed.message}`,
        });
      }
    }).catch(err => {
      console.error('Failed to fetch uploads.', err);
      alerts.add({
        type: AlertType.ERROR,
        header: (<><i className="fas fa-exclamation-triangle text-red-500" aria-hidden={true}/> Error</>),
        body: `Failed to fetch uploads.\n${err ? err.toString() : 'An internal server error occurred, please try again later.'}`,
      });
      setUploadsErrored(true);
    }).then(() => {
      setFetchingUploads(false);
    });
  }, [profile]);

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

  function rowRenderer({key, index, style}: ListRowProps): React.ReactNode {
    return (
      <div key={key} style={style}>
        <MediaPreviewBlock upload={uploads.uploads[index]}/>
      </div>
    );
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
                            <span className={`inline-block relative top-1 text-sm leading-none px-3 float-right rounded-lg border ${!uploadsErrored ? 'bg-blue-100 border-blue-200 text-blue-400' : 'bg-red-100 border-red-200 text-red-400'} opacity-95 shadow`}>
                              {fetchingUploads ? (<Spinner/>) : (
                                uploadsErrored ? (<i className="fas fa-exclamation-triangle text-xs"/>) : (uploads.uploads.length)
                              )}
                            </span>
                          </ListGroup.Item>
                          <ListGroup.Item active={path === 'bookmarks'} onClick={makeNavigator('bookmarks')}><i className="fas fa-bookmark" aria-hidden={true}/> Bookmarks</ListGroup.Item>
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
                      <div className="h-full">
                        {uploads.uploads && uploads.uploads.length ? (
                          <>
                            {uploadsErrored ? (
                              <p className="text-red-500 whitespace-pre-wrap">Failed to fetch uploads. Please try again later.</p>
                            ) : null}
                            <AutoSizer>
                              {({width, height}: Size) => (
                                <List rowCount={uploads.uploads.length} rowHeight={250} width={width} height={height} rowRenderer={rowRenderer}/>
                              )}
                            </AutoSizer>
                          </>
                        ) : (
                          uploadsErrored ? (
                            <p className="text-red-500 whitespace-pre-wrap">Failed to fetch uploads. Please try again later.</p>
                          ) : null
                        )}
                      </div>
                    </InternalRoute>
                    <InternalRoute path="bookmarks">
                      <p>hello bookmarks</p>
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
                              <tr>
                                <th className="text-right pr-2">Uploads</th>
                                <td className="text-left">{uploads.uploads.length}</td>
                              </tr>
                              <tr>
                                <th className="text-right pr-2">Bookmarks</th>
                                <td className="text-left">{0 /*TODO*/}</td>
                              </tr>
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
