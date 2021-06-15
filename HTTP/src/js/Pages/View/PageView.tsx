import * as React from 'react';
import {useEffect, useRef, useState} from 'react';
import {useParams} from 'react-router';
import {useHistory} from 'react-router-dom';
import WS from '../../classes/WS';
import {XHR} from '../../classes/XHR';
import CenteredBlockPage from '../../Components/CenteredBlockPage';

import InternalNavContext from '../../Components/InternalNav/InternalNavContext';
import InternalRoute from '../../Components/InternalNav/InternalRoute';
import InternalRouter from '../../Components/InternalNav/InternalRouter';
import InternalSwitch from '../../Components/InternalNav/InternalSwitch';
import useInternalNavigator from '../../Components/InternalNav/useInternalNavigator';
import ListGroup from '../../Components/ListGroup/ListGroup';
import ListGroupItem from '../../Components/ListGroup/ListGroupItem';
import UploadMedia from '../../Components/UploadMedia';
import {authStateSelector} from '../../Stores/AuthStore';
import NotFound from '../404/NotFound';

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

  const renderer = useRef<HTMLElement>(null);
  const ws = useRef<WS>(null);

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
    console.warn('Need to report (not yet implemented)');
  }

  function handleDelete() {
    console.warn('Need to delete (not yet implemented)');
  }

  function handleNavigation(to: string) {
    history.push(to);
  }

  function anchorNavigator(to: string) {
    return (e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) => {
      e.preventDefault();
      handleNavigation(to);
    };
  }

  function makeNavigator(to: string) {
    return (e: React.MouseEvent<HTMLButtonElement>) => {
      e.preventDefault();
      navigator.navigate(to);
    };
  }

  return (
    (!fetched ? (
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
                  <>
                    {authState.accountId === upload.owner.id ? (
                      <button onClick={handleDelete} className="text-sm text-blue-300 underline hover:text-blue-400 focus:outline-none">Delete</button>
                    ) : null}
                    <button onClick={handleReport} className="ml-1 text-sm text-blue-300 underline hover:text-blue-400 focus:outline-none">Report</button>
                  </>
                ) : null}
              </section>
              <section className="mt-2">
                <InternalNavContext.Consumer>
                  {({path = ''}) => (
                    <ListGroup>
                      <ListGroupItem active={path === 'view'} onClick={makeNavigator('view')}><i className="fas fa-image"/> View</ListGroupItem>
                      <ListGroupItem active={path === 'comments'} onClick={makeNavigator('comments')}><i className="fas fa-comment-alt"/> Comments</ListGroupItem>
                      <ListGroupItem active={path === 'edit'} onClick={makeNavigator('edit')}><i className="fas fa-pencil-alt"/> Edit</ListGroupItem>
                      <ListGroupItem active={path === 'actions'} onClick={makeNavigator('actions')}><i className="fas fa-wrench"/> Actions</ListGroupItem>
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
                  <p>hello comments</p>
                </InternalRoute>
                <InternalRoute path="edit">
                  <p>hello edit</p>
                </InternalRoute>
                <InternalRoute path="actions">
                  <p>hello actions</p>
                </InternalRoute>
                <InternalRoute path="*">
                  <div className="MediaViewer constrained">
                    <div className="MediaObject">
                      <UploadMedia ref={renderer} upload={upload.upload} media={upload.media}/>
                    </div>
                  </div>
                </InternalRoute>
              </InternalSwitch>
            </div>
          </div>
        </InternalRouter>
      ))
    ))
  );
}
