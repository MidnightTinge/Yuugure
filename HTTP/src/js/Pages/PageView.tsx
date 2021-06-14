import * as React from 'react';
import {useEffect, useRef, useState} from 'react';
import {useParams} from 'react-router';
import WS from '../classes/WS';
import {XHR} from '../classes/XHR';
import CenteredBlockPage from '../Components/CenteredBlockPage';
import NotFound from './404/NotFound';

export type PageViewProps = {
  //
};

export default function PageView(props: PageViewProps) {
  const params = useParams<{ uploadId: string }>();
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
        <div className="flex flex-grow flex-col sm:flex-row">
          <div className="flex-shrink py-2 px-1">
            <p>actions</p>
            <p>details</p>
          </div>
          <div className="flex-grow py-2 px-1">
            {upload.media.mime.toLowerCase().startsWith('image/') ? (
              <img src={`/full/${upload.upload.id}`} alt="Upload Image"/>
            ) : (
              <video src={`/full/${upload.upload.id}`} controls muted loop/>
            )}
            <p>{`${upload.media.mime.toLowerCase().startsWith('image/') ? 'image' : 'video'} viewer`}</p>
          </div>
        </div>
      ))
    ))
  );
}
