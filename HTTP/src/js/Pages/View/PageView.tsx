import * as React from 'react';
import {useEffect, useRef, useState} from 'react';
import {useParams} from 'react-router';
import {useHistory} from 'react-router-dom';
import WS from '../../classes/WS';
import {XHR} from '../../classes/XHR';
import CenteredBlockPage from '../../Components/CenteredBlockPage';
import UploadRenderer from '../../Components/UploadRenderer';
import {authStateSelector} from '../../Stores/AuthStore';
import NotFound from '../404/NotFound';

export type PageViewProps = {
  //
};

export default function PageView(props: PageViewProps) {
  const params = useParams<{ uploadId: string }>();
  const history = useHistory();
  const authState = authStateSelector();
  const [fetched, setFetched] = useState(false);
  const [error, setError] = useState<string>(null);
  const [upload, setUpload] = useState<RenderableUpload>(null);
  const [got404, setGot404] = useState(false);

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
        <div className="overflow-auto sm:overflow-hidden flex flex-grow flex-col-reverse sm:flex-row max-h-full max-w-full">
          <div className="flex flex-col flex-shrink max-h-full max-w-full px-1 bg-gray-200 border-r border-gray-300 min-w-auto sm:min-w-64">
            <section className="text-right mb-1">
              <a href={`/full/${upload.upload.id}`} className="m-0 p-0 text-blue-400 underline hover:text-blue-500 focus:outline-none mr-2" target="_blank">View Full</a>
              {authState.authed && authState.accountId === upload.owner.id ? (
                <button className="m-0 p-0 text-blue-400 underline hover:text-blue-500 focus:outline-none mr-2" onClick={handleDelete}>Delete</button>
              ) : null}
              <button className="m-0 p-0 text-blue-400 underline hover:text-blue-500 focus:outline-none" onClick={handleReport}>Report</button>
            </section>
            <section className="mb-1 text-center mx-auto">
              <table className="text-left">
                <tbody>
                  <tr>
                    <th className="font-normal text-right pr-2">Uploader:</th>
                    <td><a href={`/profile/${upload.owner.id}`} onClick={anchorNavigator(`/profile/${upload.owner.id}`)} className="text-blue-400 underline hover:text-blue-500">{upload.owner.username}</a></td>
                  </tr>
                  <tr>
                    <th className="font-normal text-right pr-2">Type:</th>
                    <td>{upload.media.mime.startsWith('image/') ? 'Image' : 'Video'}</td>
                  </tr>
                </tbody>
              </table>
            </section>
            <section>
              <div className="m-1 p-0 rounded bg-gray-300 border border-gray-400 shadow-sm">
                <div className="py-0.5 text-center text-gray-600 border-b border-gray-400">Tags</div>
                <div className="p-2">
                  <p className="text-gray-400 italic text-sm text-center select-none">Placeholder</p>
                </div>
              </div>
            </section>
          </div>

          <div className="flex-grow max-h-full-no-nav">
            <UploadRenderer upload={upload.upload} media={upload.media}/>
          </div>
        </div>
      ))
    ))
  );
}
