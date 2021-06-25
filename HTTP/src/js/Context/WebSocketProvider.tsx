import * as React from 'react';
import {useMemo} from 'react';
import namedContext from '../classes/NamedContext';
import RoomHelper from '../classes/RoomHelper';
import WS from '../classes/WS';

export type WebSocketProviderState = {
  ws: WS;
  rooms: RoomHelper;
}

export const WebSocketContext = namedContext<WebSocketProviderState>('WebSocketProviderContext', {ws: null, rooms: null});

export default function WebSocketProvider({children}: any) {
  const ctxMemo = useMemo<WebSocketProviderState>(() => {
    let ws = new WS();
    let rooms = new RoomHelper(ws);
    ws.connect();

    return {ws, rooms};
  }, []);

  return (
    <WebSocketContext.Provider value={{...ctxMemo}}>
      {children}
    </WebSocketContext.Provider>
  );
}

