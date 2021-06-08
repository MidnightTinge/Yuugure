import {createStore, applyMiddleware} from 'redux';
import {useSelector} from 'react-redux';
import {XHR} from '../classes/XHR';
import {mwFnAcceptor} from './StoreUtils';

export type AuthStoreState = {
  authed: boolean;
  fetched: boolean;
  fetching: boolean;
  user?: any;
  error?: string;
}

const ACTIONS = {
  SET_STATE: 'auth/setState',
  SET_PARTIAL: 'auth/setPartial',

  SET_ERROR: 'auth/setError',

  SET_FETCHED: 'auth/setFetched',
  SET_FETCHING: 'auth/setFetching',
};

const DEFAULT_STATE: AuthStoreState = {
  authed: false,
  fetched: false,
  fetching: false,
  user: null,
  error: null,
};

export function AuthStateReducer(state: AuthStoreState = {...DEFAULT_STATE}, action: ReducerAction) {
  switch (action.type) {
    case ACTIONS.SET_STATE: {
      return {
        ...action.payload,
      };
    }
    case ACTIONS.SET_PARTIAL: {
      return {
        ...state,
        ...action.payload,
      };
    }
    case ACTIONS.SET_ERROR: {
      return {
        ...state,
        error: action.payload,
      };
    }
    case ACTIONS.SET_FETCHED: {
      return {
        ...state,
        fetched: action.payload,
      };
    }
    case ACTIONS.SET_FETCHING: {
      return {
        ...state,
        fetching: action.payload,
      };
    }
  }

  return state;
}

export const authStateStore = createStore(AuthStateReducer, DEFAULT_STATE, applyMiddleware(mwFnAcceptor));

export const reloadAuthState = () => dispatch('_', _reloadAuth);

export const setFetched = (fetched: boolean) => dispatch(ACTIONS.SET_FETCHED, fetched);
export const setFetching = (fetching: boolean) => dispatch(ACTIONS.SET_FETCHING, fetching);
export const setError = (error: string) => dispatch(ACTIONS.SET_FETCHING, error);
export const setState = (state: Required<AuthStoreState>) => dispatch(ACTIONS.SET_STATE, state);
export const setPartial = (state: Partial<AuthStoreState>) => dispatch(ACTIONS.SET_PARTIAL, state);

export const authStateSelector = () => useSelector<AuthStoreState, AuthStoreState>(state => ({...state}));

function _reloadAuth(dispatch: Function, getState: Function) {
  setPartial({
    fetching: true,
    fetched: false,
  });
  XHR.for('/auth/check').get().getJson<RouterResponse<AuthStateResponse>>().then(data => {
    if (data && data.data && Array.isArray(data.data.AuthStateResponse) && data.data.AuthStateResponse[0]) {
      const state: AuthStateResponse = data.data.AuthStateResponse[0];
      setState({
        authed: state.authenticated,
        user: state.account_id,
        error: null,
        fetched: true,
        fetching: false,
      });
    } else {
      console.error('Invalid response from the check endpoint, cannot confirm authentication state.', data);
    }
  }).catch(err => {
    console.error('[AuthStore] Failed to reload.', err);
    setError(err.toString());
  }).then(() => {
    setPartial({
      fetching: false,
      fetched: true,
    });
  });
}

function dispatch(t: string, p: any) {
  authStateStore.dispatch({
    type: t,
    payload: p,
  });
}
