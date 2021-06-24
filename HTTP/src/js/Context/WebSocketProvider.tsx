import * as React from 'react';
import {useMemo} from 'react';
import namedContext from '../classes/NamedContext';
import WS from '../classes/WS';

export type WebSocketProviderState = {
  ws: WS;
}

export const WebSocketContext = namedContext<WebSocketProviderState>('WebSocketProviderContext', {ws: null});

export default function WebSocketProvider({children}: any) {
  const ws = useMemo(() => {
    let ws = new WS();
    ws.connect();

    return ws;
  }, []);

  return (
    <WebSocketContext.Provider value={{ws}}>
      {children}
    </WebSocketContext.Provider>
  );
}

