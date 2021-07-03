import {useReducer} from 'react';
import {Alert} from './Alert/Alert';

export enum ACTIONS {
  SET = 'alerts/set',
  ADD = 'alerts/add',
  REMOVE = 'alerts/rem',
  MODIFY = 'alerts/modify',
  CLOSE = 'alerts/close',
  CLOSE_ALL = 'alerts/close_all',
  CLEAR = 'alerts/clear',
}

type ReducerAction<T> = {
  type: T,
  payload?: any,
};

type AlertsState = {
  alerts: Alert[];
}

function AlertReducer(state: AlertsState, {type, payload}: ReducerAction<ACTIONS>): AlertsState {
  switch (type) {
    case ACTIONS.SET: {
      return {
        alerts: [...payload],
      };
    }
    case ACTIONS.ADD: {
      return {
        alerts: [...state.alerts, payload],
      };
    }
    case ACTIONS.REMOVE: {
      return {
        alerts: state.alerts.filter(x => x.id !== payload),
      };
    }
    case ACTIONS.MODIFY: {
      return {
        alerts: state.alerts.map(x => x.id === payload.id ? {...x, ...payload} : x),
      };
    }
    case ACTIONS.CLOSE: {
      return {
        alerts: state.alerts.map(x => x.id === payload ? {...x, show: false} : x),
      };
    }
    case ACTIONS.CLOSE_ALL: {
      return {
        alerts: state.alerts.map(x => ({...x, show: false})),
      };
    }
    case ACTIONS.CLEAR: {
      return {
        alerts: [],
      };
    }
    default: {
      throw new Error('Got an invalid AlertReducer type:' + type);
    }
  }
}

const _default: AlertsState = {
  alerts: [],
};

export default function useAlertReducer() {
  return useReducer(AlertReducer, {..._default}, () => ({..._default}));
}
