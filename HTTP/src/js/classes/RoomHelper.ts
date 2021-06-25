import ParseBinaryPacket from './ParseBinaryPacket';
import WS from './WS';

const ACK = 1;
const UNKNOWN_TYPE = 99;

const ACK_SUB = 1;
const ACK_UNSUB = 2;

/**
 * A helper class that interfaces with our WebSocket class and manages room state.
 */
export default class RoomHelper {
  _ws: WS;
  _rooms: Record<string, boolean> = {};

  constructor(ws: WS) {
    this._ws = ws;
    this._ws.addEventHandler('open', () => {
      for (let [room] of Object.entries(this._rooms)) {
        this._ws.emit('sub', {room});
      }
    });
    this._ws.addEventHandler('message', ev => {
      if (typeof ev.data !== 'string') {
        if (ev.data instanceof Blob) {
          (ev.data as Blob).arrayBuffer().then(data => {
            this._handleBlobResponse(data);
          });
        } else if (ev.data instanceof ArrayBuffer) {
          this._handleBlobResponse(ev.data);
        }
      }
    });
  }

  _handleBlobResponse(data: ArrayBuffer) {
    let bytes = new Int8Array(data);
    let parsed = ParseBinaryPacket(bytes);
    if (parsed) {
      if (parsed.type === ACK) {
        if (parsed.header[0] === ACK_SUB) {
          this._rooms[decodeUTF8(parsed.payload)] = true;
        } else if (parsed.header[0] === ACK_UNSUB) {
          delete this._rooms[decodeUTF8(parsed.payload)];
        }
      }
    }
  }

  join(room: string) {
    this._ws.emit('sub', {room});
    this._rooms[room] = true;
  }

  leave(room: string) {
    this._ws.emit('unsub', {room});
    this._rooms[room] = false;
    delete this._rooms[room];
  }
}


/**
 * Unmarshals a string from a UInt8Array
 *
 * @param bytes The bytes to convert to a string
 * @author https://github.com/pascaldekloe
 */
function decodeUTF8(bytes: number[]) {
  // https://gist.github.com/pascaldekloe/62546103a1576803dade9269ccf76330#file-utf8-js-L33
  let i = 0, s = '';
  while (i < bytes.length) {
    let c = bytes[i++];
    if (c > 127) {
      if (c > 191 && c < 224) {
        if (i >= bytes.length)
          throw new Error('UTF-8 decode: incomplete 2-byte sequence');
        c = (c & 31) << 6 | bytes[i++] & 63;
      } else if (c > 223 && c < 240) {
        if (i + 1 >= bytes.length)
          throw new Error('UTF-8 decode: incomplete 3-byte sequence');
        c = (c & 15) << 12 | (bytes[i++] & 63) << 6 | bytes[i++] & 63;
      } else if (c > 239 && c < 248) {
        if (i + 2 >= bytes.length)
          throw new Error('UTF-8 decode: incomplete 4-byte sequence');
        c = (c & 7) << 18 | (bytes[i++] & 63) << 12 | (bytes[i++] & 63) << 6 | bytes[i++] & 63;
      } else throw new Error('UTF-8 decode: unknown multibyte start 0x' + c.toString(16) + ' at index ' + (i - 1));
    }
    if (c <= 0xffff) s += String.fromCharCode(c);
    else if (c <= 0x10ffff) {
      c -= 0x10000;
      s += String.fromCharCode(c >> 10 | 0xd800);
      s += String.fromCharCode(c & 0x3FF | 0xdc00);
    } else throw new Error('UTF-8 decode: code point 0x' + c.toString(16) + ' exceeds UTF-16 reach');
  }
  return s;
}
