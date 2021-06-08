import {Store} from 'redux';

export function mwFnAcceptor(store: Store) {
  return (next: Function) => (action: ReducerAction) => {
    if (typeof action.payload === 'function') {
      return action.payload(store.dispatch, store.getState);
    }

    return next(action);
  };
}
