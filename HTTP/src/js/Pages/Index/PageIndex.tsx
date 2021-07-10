import KY from '../../classes/KY';
import * as React from 'react';
import {useContext, useEffect, useReducer} from 'react';
import {useHistory} from 'react-router-dom';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Util from '../../classes/Util';

import LoadingPing from '../../Components/LoadingPing';
import MediaPreview from '../../Components/MediaPreview/MediaPreview';
import {AuthStateContext} from '../../Context/AuthStateProvider';

export type PageIndexProps = {
  //
};

type IndexUploadsState = {
  fetching: boolean;
  error: string;
  uploads: RenderableUpload[];
}

function IndexUploadReducer(state: IndexUploadsState, action: ReducerAction): IndexUploadsState {
  switch (action.type) {
    case 'set': {
      return {
        ...action.payload,
      };
    }
    case 'setPartial': {
      return {
        ...state,
        ...action.payload,
      };
    }
  }
  return state;
}

export default function PageIndex(props: PageIndexProps) {
  const [indexState, indexStateD] = useReducer(IndexUploadReducer, {uploads: [], fetching: true, error: null});
  const {state} = useContext(AuthStateContext);
  const history = useHistory();

  useEffect(() => {
    indexStateD({type: 'setPartial', payload: {fetching: true, error: null}});
    KY.get('/api/upload/index').json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<BulkRenderableUpload>(data);
      indexStateD({
        type: 'set',
        payload: {
          fetching: false,
          error: consumed.message,
          uploads: consumed.success ? [...Util.mapBulkUploads(consumed.data[0])] : [],
        },
      });
    }).catch(err => {
      indexStateD({type: 'setPartial', payload: {fetching: false, error: err?.toString() ?? 'An internal server error occurred.'}});
    });
  }, []);

  function handleUploadClick(e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) {
    e.preventDefault();
    history.push('/upload');
  }

  return (
    indexState.fetching ? (
      <div className="flex flex-col justify-center items-center h-full w-full">
        <LoadingPing/>
      </div>
    ) : (
      <div className="p-3">
        <h1 className="text-2xl font-medium">Recent Uploads</h1>
        <hr className="mb-4 mt-1"/>
        {indexState.uploads && indexState.uploads.length ? (
          <div className="grid gap-4" style={{gridTemplateColumns: 'repeat(auto-fit, 210px)'}}>
            {indexState.uploads.map((upload, idx) => (<MediaPreview upload={upload} key={idx}/>))}
          </div>
        ) : (
          <p className="text-gray-700">No uploads yet! {state.authed ? (<a className="text-blue-500 hover:text-blue-700 underline" href="/uploads" onClick={handleUploadClick}>Add some?</a>) : null}</p>
        )}
      </div>
    )
  );
}
