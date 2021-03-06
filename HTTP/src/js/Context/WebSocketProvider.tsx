import * as React from 'react';
import {useMemo} from 'react';
import namedContext from '../classes/NamedContext';
import RoomHelper from '../classes/RoomHelper';
import WS from '../classes/WS';
import {Alert, AlertType} from '../Components/Alerts/Alert/Alert';
import {useAlerts} from '../Components/Alerts/AlertsProvider';

export type WebSocketProviderState = {
  ws: WS;
  rooms: RoomHelper;
}

export const WebSocketContext = namedContext<WebSocketProviderState>('WebSocketProviderContext', {ws: null, rooms: null});

function DisconnectBody({remaining}: { remaining: number }) {
  // format the difference as "s.sss"
  const fm = remaining >= 1e3 ? `${remaining / 1e3 >> 0}.${remaining % 1e3 >> 0}s` : `0.${remaining}s`;

  return (
    <p>Lost connection to the server, will attempt to re-connect in {fm}...</p>
  );
}

export const WebSocketProvider: React.FC = ({children}: { children: React.ReactNode }) => {
  const alerts = useAlerts();
  const ctxMemo = useMemo<WebSocketProviderState>(() => {
    const ws = new WS();
    const rooms = new RoomHelper(ws);

    let intvl: number = null;
    let disconnectAlert: Alert = null;

    ws.addEventHandler('timeout', (attempts: number, timeout: number) => {
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
        const target = Date.now() + timeout;
        intvl = (setInterval(() => {
          if (disconnectAlert != null) {
            // get the remaining ms between target and now with a minimum of 0ms.
            const remaining = Math.max(0, target - Date.now());
            alerts.update({
              ...disconnectAlert,
              body: (<DisconnectBody remaining={remaining}/>),
            });
          }
        }, 31) as unknown) as number;
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

  return (
    <WebSocketContext.Provider value={{...ctxMemo}}>
      {children}
    </WebSocketContext.Provider>
  );
};

export default WebSocketProvider;
