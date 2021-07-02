import * as React from 'react';
import {useCallback, useContext, useEffect, useReducer} from 'react';
import namedContext from '../classes/NamedContext';
import {XHR} from '../classes/XHR';

export type AuthState = {
  authed: boolean;
  account: SafeAccount;
  error: string;
};

export type AuthContextProps = {
  state: AuthState,
  reloadAuth: () => Promise<void>,
  dispatch: React.Dispatch<ReducerAction>,
};

const defaultAuthState: AuthState = {
  authed: false,
  account: null,
  error: null,
};

const defaultReducerState: AuthContextProps = {
  state: {...defaultAuthState},
  reloadAuth: null,
  dispatch: null,
};

const ACTIONS = Object.freeze({
  FETCHED: 'fetched',
  ERROR: 'error',
});

export const AuthStateContext = namedContext<AuthContextProps>('AuthState', {...defaultReducerState});
const AuthStateReducer = (state: AuthState, action: ReducerAction): AuthState => {
  switch (action.type) {
    case ACTIONS.FETCHED: {
      return {
        ...state,
        ...action.payload,
      };
    }
    case ACTIONS.ERROR: {
      return {
        ...state,
        error: action.payload,
      };
    }
  }
  return state;
};

export default function AuthStateProvider({children}: any) {
  const [state, dispatch] = useReducer(AuthStateReducer, {...defaultAuthState}, () => ({...defaultAuthState}));

  const reloadAuth = useCallback(async () => {
    try {
      let consumed = await XHR.for(`/auth/check`).get().getRouterResponse<AuthStateResponse>();
      if (consumed.success) {
        let [resp] = consumed.data;
        dispatch({type: 'fetched', payload: {authed: resp.authenticated, account: resp.account, error: null}});
      } else {
        dispatch({type: 'fetched', payload: {error: consumed.message, account: null, authed: false}});
      }
    } catch (e) {
      dispatch({type: 'error', payload: e.toString()});
    }
  }, []);

  useEffect(() => {
    reloadAuth().catch(console.error);
  }, []);

  return (
    <AuthStateContext.Provider value={{state, reloadAuth, dispatch}}>
      {children}
    </AuthStateContext.Provider>
  );
}

export function useAuthState() {
  const context = useContext(AuthStateContext);

  return context.state;
}
