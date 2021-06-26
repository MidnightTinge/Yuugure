import * as React from 'react';
import {useContext, useEffect, useReducer} from 'react';
import {useHistory} from 'react-router-dom';
import {XHR} from '../../classes/XHR';
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
    XHR.for(`/api/upload/index`).get().getJson().then(res => {
      if (res) {
        if (res.code === 200) {
          indexStateD({
            type: 'set',
            payload: {
              fetching: false,
              error: null,
              uploads: [...res.data.RenderableUpload],
            },
          });
        } else {
          indexStateD({type: 'setPartial', payload: {fetching: false, error: 'An internal server error occurred. Please try again later.'}});
        }
      } else {
        indexStateD({type: 'setPartial', payload: {fetching: false, error: 'Received an invalid response. Please try again later.'}});
      }
    }).catch(err => {
      indexStateD({type: 'setPartial', payload: {fetching: false, error: err.toString()}});
    });
  }, []);

  function handleUploadClick(e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) {
    e.preventDefault();
    history.push('/upload');
  }

  return (
    indexState.fetching ? (
      <div className="flex flex-col justify-center items-center h-full w-full">
        <i className="fas fa-hourglass-half text-gray-400 fa-5x animate-ping" aria-hidden={true}/><span className="sr-only">Loading...</span>
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
