import {useReducer} from 'react';

export type CommentsState = {
  fetching: boolean;
  error: string;
  comments: RenderableComment[];
}

function CommentsReducer(state: CommentsState, action: { type: string, payload?: any }): CommentsState {
  switch (action.type) {
    case 'fetching/fetching': {
      return {
        ...state,
        fetching: action.payload,
      };
    }
    case 'fetching/error': {
      return {
        ...state,
        error: action.payload,
      };
    }
    case 'fetching/set': {
      return {
        ...state,
        ...action.payload,
      };
    }
    case 'comments/add': {
      let payload = Array.isArray(action.payload) ? action.payload : [action.payload];
      return {
        ...state,
        comments: [...payload, ...state.comments],
      };
    }
    case 'comments/remove': {
      return {
        ...state,
        comments: state.comments.filter(x => x.id !== action.payload.id),
      };
    }
    case 'comments/update': {
      return {
        ...state,
        comments: state.comments.map(x => x.id === action.payload.id ? {...x, ...action.payload} : x),
      };
    }
    case 'comments/set': {
      let payload = Array.isArray(action.payload) ? action.payload : [action.payload];
      return {
        ...state,
        comments: [...payload],
      };
    }
  }

  return state;
}

const _defaultState: CommentsState = {
  fetching: false,
  error: null,
  comments: [],
};

export default function useCommentsReducer() {
  return useReducer(CommentsReducer, {..._defaultState}, () => ({..._defaultState}));
}
