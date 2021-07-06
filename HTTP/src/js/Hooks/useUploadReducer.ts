import {Dispatch, useReducer} from 'react';

export type UploadReducerHook = [UploadState, Dispatch<ReducerAction>, UploadActions];

type UploadState = {
  uploads: RenderableUpload[];
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
    case 'remove': {
      return {
        ...state,
        uploads: state.uploads.filter(x => x.upload.id !== payload[0].upload.id),
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

type UploadActions = {
  set: (uploads: Arrayable<RenderableUpload>) => void;
  add: (uploads: Arrayable<RenderableUpload>) => void;
  remove: (upload: RenderableUpload) => void;
}

function _makeActions(dispatch: Dispatch<ReducerAction>): UploadActions {
  return {
    set: (uploads: Arrayable<RenderableUpload>) => {
      if (!Array.isArray(uploads)) {
        uploads = [uploads];
      }
      dispatch({
        type: 'set',
        payload: uploads,
      });
    },
    add: (upload: Arrayable<RenderableUpload>) => {
      if (!Array.isArray(upload)) {
        upload = [upload];
      }
      dispatch({
        type: 'add',
        payload: upload,
      });
    },
    remove: (upload: RenderableUpload) => {
      dispatch({
        type: 'remove',
        payload: upload,
      });
    },
  };
}

export default function useUploadReducer(): UploadReducerHook {
  const [reducer, dispatch] = useReducer(UploadReducer, {uploads: []}, () => ({uploads: []}));

  return [reducer, dispatch, _makeActions(dispatch)];
}
