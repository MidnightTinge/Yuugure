import * as React from 'react';
import {useEffect, useMemo, useReducer, useState} from 'react';
import {useHistory, useParams} from 'react-router';

import {AutoSizer, List, ListRowProps, Size} from 'react-virtualized';
import {XHR} from '../../classes/XHR';
import CenteredBlockPage from '../../Components/CenteredBlockPage';
import InternalNavContext from '../../Components/InternalNav/InternalNavContext';
import InternalRoute from '../../Components/InternalNav/InternalRoute';
import InternalRouter from '../../Components/InternalNav/InternalRouter';
import InternalSwitch from '../../Components/InternalNav/InternalSwitch';
import useInternalNavigator from '../../Components/InternalNav/useInternalNavigator';
import ListGroup from '../../Components/ListGroup/ListGroup';
import ListGroupItem from '../../Components/ListGroup/ListGroupItem';
import MediaPreviewBlock from '../../Components/MediaPreview/MediaPreviewBlock';
import {CloseSource} from '../../Components/Modal/Modal';
import ReportModal from '../../Components/modals/ReportModal';
import Spinner from '../../Components/Spinner';
import {authStateSelector} from '../../Stores/AuthStore';
import NotFound from '../404/NotFound';
import AccountSettings from './AccountSettings';

type UploadState = {
  uploads: RenderableUpload[];
};

export type PageProfileProps = {
  self?: boolean;
};

function UploadReducer(state: UploadState, action: { type: string, payload?: Arrayable<RenderableUpload> }): UploadState {
  let payload = Array.isArray(action.payload) ? action.payload : [action.payload];

  switch (action.type) {
    case 'add': {
      return {
        ...state,
        uploads: [...state.uploads, ...payload],
      };
    }
    case 'set': {
      return {
        ...state,
        uploads: [...payload],
      };
    }
  }
}

export default function PageProfile(props: PageProfileProps) {
  const authState = authStateSelector();
  const params = useParams<{ accountId: string }>();
  const navigator = useInternalNavigator(true);
  const history = useHistory();

  const [fetching, setFetching] = useState(false);
  const [fetched, setFetched] = useState(false);
  const [error, setError] = useState<string>(null);
  const [got404, setGot404] = useState(false);

  const [fetchingUploads, setFetchingUploads] = useState(false);
  const [uploadsError, setUploadsError] = useState<string>(null);

  const [profile, setProfile] = useState<ProfileResponse>(null);
  const [uploads, uploadsDispatch] = useReducer(UploadReducer, {uploads: []}, () => ({uploads: []}));

  const [showReport, setShowReport] = useState(false);

  const [lastAccountId, setLastAccountId] = useState<string>(null);

  const accountId = useMemo(() => (props.self || (authState.authed && params.accountId && Number(params.accountId) === authState.account.id)) ? '@me' : params.accountId, [params, authState]);

  // note: It's possible for our authState to not be initialized yet, in which case this can flag
  //       as `false` incorrectly. the back-end corrects for this automatically on the profile API
  //       request.
  const isSelf = props.self || (authState.authed && params.accountId && Number(params.accountId) === authState.account.id);

  useEffect(function mounted() {
    // TODO listen to profile-specific events on WS
    // ws.subscribe(`profile:${params.accountId}`);
    setFetching(true);
    setFetched(false);

    XHR.for(`/api/profile/${accountId}`).get().getJson<RouterResponse<ProfileResponse>>().then(res => {
      if (res.code === 200 && Array.isArray(res.data.ProfileResponse) && res.data.ProfileResponse[0]) {
        setProfile(res.data.ProfileResponse[0]);
      } else if (res.code === 404) {
        setGot404(true);
      } else if (res.code === 401) {
        setError('You do not have permission to view this resource.');
      } else {
        setError('An internal server error occurred. Please try again later.');
      }
    }).catch(err => {
      console.error('Failed to fetch profile.', err);
      setError(err.toString());
    }).then(() => {
      setFetched(true);
      setFetching(false);
    });

    return function unmounted() {
      // TODO cleanup profile-specific WS events

      setFetching(false);
      setFetched(false);
      setGot404(false);
      setError(null);
      setFetchingUploads(false);
      setUploadsError(null);
      setShowReport(false);
    };
  }, [accountId]);

  useEffect(() => {
    if (accountId === lastAccountId) return;
    setLastAccountId(accountId);

    uploadsDispatch({type: 'set', payload: []});
    setFetchingUploads(true);
    XHR.for(`/api/profile/${accountId}/uploads`).get().getJson<RouterResponse<RenderableUpload>>().then(res => {
      if (res.code === 200 && Array.isArray(res.data.RenderableUpload)) {
        uploadsDispatch({type: 'set', payload: [...res.data.RenderableUpload]});
      } else if (res.code === 404) {
        setUploadsError('Could not find any uploads.');
      } else if (res.code === 401) {
        setUploadsError('You do not have permission to fetch these uploads.');
      } else {
        setUploadsError('An internal server error occurred. Please try again later.');
      }
    }).catch(err => {
      console.error('Failed to fetch uploads.', err);
      setUploadsError(err.toString());
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

  function rowRenderer({key, index, isScrolling, isVisible, style}: ListRowProps): React.ReactNode {
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
              <i className="fas fa-address-card fa-4x text-gray-300 animate-ping"/>
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
                            <span className="inline-block relative top-1 text-sm leading-none px-3 float-right rounded-lg bg-blue-100 border border-blue-200 text-blue-400 opacity-95 shadow">
                              {fetchingUploads ? (<Spinner />) : (uploads.uploads.length)}
                            </span>
                          </ListGroup.Item>
                          <ListGroup.Item active={path === 'likes'} onClick={makeNavigator('likes')}><i className="fas fa-heart" aria-hidden={true}/> Likes</ListGroup.Item>
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
                        <AutoSizer>
                          {({width, height}: Size) => (
                            <List rowCount={uploads.uploads.length} rowHeight={250} width={width} height={height} rowRenderer={rowRenderer}/>
                          )}
                        </AutoSizer>
                      </div>
                    </InternalRoute>
                    <InternalRoute path="likes">
                      <p>hello likes</p>
                    </InternalRoute>
                    <InternalRoute path="votes">
                      <p>hello votes</p>
                    </InternalRoute>
                    <InternalRoute path="settings">
                      <AccountSettings account={authState.account} />
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
                                <th className="text-right pr-2">Likes</th>
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
