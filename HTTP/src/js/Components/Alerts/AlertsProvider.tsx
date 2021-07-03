import * as React from 'react';
import {useContext, useMemo} from 'react';
import {createPortal} from 'react-dom';
import namedContext from '../../classes/NamedContext';
import Util from '../../classes/Util';
import {Alert} from './Alert/Alert';
import AlertContainer from './AlertContainer';
import useAlertReducer, {ACTIONS} from './AlertsReducer';

// make ID optional, if it doesn't exist we'll call Util#mkid().
type _Alert = Omit<Alert, 'id' | 'show'> & { id?: string };
type AlertContextProps = {
  add: (alert: _Alert) => Alert;
  update: (alert: Alert) => Alert;
  remove: (id: string) => void;
  close: (id: string) => void;
  closeAll: () => void;
  clear: () => void;
}

export const AlertContext = namedContext<AlertContextProps>('AlertContext');

export function useAlertContext() {
  return useContext(AlertContext);
}

export default function AlertsProvider({children}: any) {
  const [alerts, dispatch] = useAlertReducer();
  const ctxMemo = useMemo<AlertContextProps>(() => {
    return {
      add: (alert: _Alert) => {
        let payload: Alert = {
          ...alert,
          dismissable: alert.dismissable !== false, // default to true
          show: true,
          id: alert.id ? alert.id : Util.mkid(),
        };

        dispatch({
          type: ACTIONS.ADD,
          payload,
        });

        return payload;
      },
      update: (alert: Alert) => {
        let payload: Alert = {...alert};
        dispatch({
          type: ACTIONS.MODIFY,
          payload,
        });

        return payload;
      },
      remove: (id: string) => {
        dispatch({type: ACTIONS.REMOVE, payload: id});
      },
      close: (id: string) => {
        dispatch({type: ACTIONS.CLOSE, payload: id});
      },
      closeAll: () => {
        // sets all alerts to {show=false} so they animate out
        dispatch({type: ACTIONS.CLOSE_ALL});
      },
      clear: () => {
        // immediately unmounts all components
        dispatch({type: ACTIONS.CLEAR});
      },
    };
  }, []);

  function handleCloseRequest(alert: Alert) {
    ctxMemo.close(alert.id);
  }

  function handleAlertClosed(alert: Alert) {
    ctxMemo.remove(alert.id);
  }

  return (
    <>
      <AlertContext.Provider value={ctxMemo}>
        {children}
      </AlertContext.Provider>
      {createPortal(<AlertContainer alerts={alerts.alerts} onClose={handleAlertClosed} onCloseRequest={handleCloseRequest}/>, document.body)}
    </>
  );
}
