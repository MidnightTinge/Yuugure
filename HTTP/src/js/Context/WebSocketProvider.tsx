import * as React from 'react';
import {useEffect, useMemo} from 'react';
import namedContext from '../classes/NamedContext';
import RoomHelper from '../classes/RoomHelper';
import WS, {CancelHolder} from '../classes/WS';
import {Alert, AlertType} from '../Components/Alerts/Alert/Alert';
import {useAlertContext} from '../Components/Alerts/AlertsProvider';

export type WebSocketProviderState = {
  ws: WS;
  rooms: RoomHelper;
}

export const WebSocketContext = namedContext<WebSocketProviderState>('WebSocketProviderContext', {ws: null, rooms: null});

export default function WebSocketProvider({children}: any) {
  const alerts = useAlertContext();
  const ctxMemo = useMemo<WebSocketProviderState>(() => {
    let ws = new WS();
    let rooms = new RoomHelper(ws);

    let disconnectAlert: Alert = null;
    ws.addEventHandler('reconnect', (attempts: number, canceler: CancelHolder) => {
      if (disconnectAlert == null) {
        disconnectAlert = alerts.add({
          type: AlertType.ERROR,
          header: 'Disconnected',
          body: 'Lost connection to the server, attempting to re-establish...',
        });
      } else if (attempts < 20) {
        alerts.update({
          ...disconnectAlert,
          body: `Lost connection to the server, attempting to re-establish (${attempts} attempts)...`,
        });
      } else {
        canceler.cancel = true;
        alerts.update({
          ...disconnectAlert,
          body: `Lost connection to the server and failed 20 reconnection attempts. Automatic retry has been disabled, please reload to retry manually.`,
        });
      }
    });
    ws.addEventHandler('open', () => {
      if (disconnectAlert != null) {
        alerts.close(disconnectAlert.id);
        disconnectAlert = null;
      }
    });

    ws.connect();
    return {ws, rooms};
  }, []);

  // Ensure our close icon is loaded into the browser's memcache on mount so that if the user's
  // internet drops it will still render. Inserting the icon will force the browser to fetch the
  // font.
  useEffect(() => {
    const closeIcon = document.createElement('i');
    closeIcon.classList.add('fas', 'fa-times');

    // hide from the user
    const iconWrapper = document.createElement('div');
    iconWrapper.style.width = '1px';
    iconWrapper.style.height = '1px';
    iconWrapper.style.margin = '-1px';
    iconWrapper.style.padding = '0px';
    iconWrapper.style.overflow = 'hidden';
    iconWrapper.style.clip = 'rect(0,0,0,0)';
    iconWrapper.style.border = '0';
    iconWrapper.style.position = 'absolute';
    iconWrapper.style.whiteSpace = 'nowrap';
    iconWrapper.style.borderWidth = '0';

    // inject
    iconWrapper.appendChild(closeIcon);
    document.body.appendChild(iconWrapper);

    // remove once mounted
    setTimeout(() => {
      iconWrapper.remove();
    }, 10);
  }, []);

  return (
    <WebSocketContext.Provider value={{...ctxMemo}}>
      {children}
    </WebSocketContext.Provider>
  );
}

