import * as React from 'react';
import {useEffect, useMemo} from 'react';
import namedContext from '../classes/NamedContext';
import RoomHelper from '../classes/RoomHelper';
import WS, {CancelHolder} from '../classes/WS';
import {Alert, AlertType} from '../Components/Alerts/Alert/Alert';
import {useAlerts} from '../Components/Alerts/AlertsProvider';

export type WebSocketProviderState = {
  ws: WS;
  rooms: RoomHelper;
}

export const WebSocketContext = namedContext<WebSocketProviderState>('WebSocketProviderContext', {ws: null, rooms: null});

function DisconnectBody({remaining}: { remaining: number }) {
  // format the difference as "s.sss"
  let fm = remaining >= 1e3 ? `${remaining / 1e3 >> 0}.${remaining % 1e3 >> 0}s` : `0.${remaining}s`;

  return (
    <p>Lost connection to the server, will attempt to re-connect in {fm}...</p>
  );
}

export default function WebSocketProvider({children}: any) {
  const alerts = useAlerts();
  const ctxMemo = useMemo<WebSocketProviderState>(() => {
    let ws = new WS();
    let rooms = new RoomHelper(ws);

    let intvl: number = null;
    let disconnectAlert: Alert = null;

    ws.addEventHandler('timeout', (attempts: number, timeout: number, canceler: CancelHolder) => {
      if (disconnectAlert == null) {
        disconnectAlert = alerts.add({
          type: AlertType.ERROR,
          header: 'Disconnected',
          body: (<DisconnectBody remaining={timeout}/>),
        });
      } else {
        alerts.update({
          ...disconnectAlert,
          body: (<DisconnectBody remaining={timeout}/>),
        });
      }

      if (intvl != null) {
        clearInterval(intvl);
        intvl = null;
      }

      if (timeout > 1) {
        let target = Date.now() + timeout;
        intvl = setInterval(() => {
          if (disconnectAlert != null) {
            // get the remaining ms between target and now with a minimum of 0ms.
            let remaining = Math.max(0, target - Date.now());
            alerts.update({
              ...disconnectAlert,
              body: (<DisconnectBody remaining={remaining}/>),
            });
          }
        }, 31);
      }
    });
    ws.addEventHandler('reconnect', (attempts: number) => {
      if (intvl != null) {
        clearInterval(intvl);
        intvl = null;
      }
      if (disconnectAlert != null) {
        alerts.update({
          ...disconnectAlert,
          body: (
            <p>Attempting to reconnect to the server (attempt #{attempts}...)</p>
          ),
        });
      }
    });
    ws.addEventHandler('open', () => {
      if (disconnectAlert != null) {
        alerts.close(disconnectAlert.id);
        disconnectAlert = null;
      }
      if (intvl != null) {
        clearInterval(intvl);
        intvl = null;
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
