import * as React from 'react';
import {useCallback, useContext, useEffect, useReducer} from 'react';
import Account from '../classes/Account';
import KY from '../classes/KY';
import namedContext from '../classes/NamedContext';
import RouterResponseConsumer from '../classes/RouterResponseConsumer';

export type AuthState = {
  authed: boolean;
  account: Account;
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

export const AuthStateProvider: React.FC = ({children}: any) => {
  const [state, dispatch] = useReducer(AuthStateReducer, {...defaultAuthState}, () => ({...defaultAuthState}));

  const reloadAuth = useCallback(async () => {
    try {
      const data = await KY.get('/auth/check').json<RouterResponse>();
      const consumed = RouterResponseConsumer<AuthStateResponse>(data);

      if (consumed.success) {
        const [resp] = consumed.data;
        dispatch({
          type: 'fetched', payload: {
            authed: resp.authenticated,
            account: new Account(resp.account),
            error: null,
          },
        });
      } else {
        dispatch({
          type: 'fetched', payload: {
            error: consumed.message,
            account: null,
            authed: false,
          },
        });
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
};

export default AuthStateProvider;

export function useAuthState(): AuthState {
  const context = useContext(AuthStateContext);

  return context.state;
}
