import EventableBase from './EventableBase';

export type CancelHolder = {
  cancel: boolean;
};

export class WS extends EventableBase {
  readonly #url: string;
  #ws: WebSocket;
  #disconnecting: boolean = false;
  #connectionAttempts: number = 0;
  #sendBuff: string[] = [];

  constructor(url?: string) {
    super();
    if (url == null || !(typeof url === 'string') || url.trim().length == 0) {
      url = `${document.location.protocol === 'https:' ? 'wss' : 'ws'}://${document.location.host}/ws`;
    }
    this.#url = url;
  }

  emit(type: string, payload?: any) {
    this.send(Object.assign(payload == null ? {} : payload, {type}));
  }

  send(obj: any): boolean {
    if (typeof obj !== 'string') {
      obj = JSON.stringify(obj);
    }
    if (this.#ws != null && this.#ws.readyState == WebSocket.OPEN) {
      this.#ws.send(obj);
      return true;
    } else {
      this.#sendBuff.push(obj);
      return false;
    }
  }

  connect() {
    if (this.#ws != null) {
      this.clearHandlers();
      try {
        this.#ws.close();
      } catch (e) {
        //
      }
      this.#ws = null;
    }

    this.#disconnecting = false;
    this.#ws = new WebSocket(this.#url);
    this.addHandlers();
  }

  disconnect() {
    this.#disconnecting = true;
    if (this.#ws != null) {
      this.#ws.close();
    }
  }

  private fire(type: string, packet: any) {
    if (packet == null) return;
    delete packet.type;

    this.invoke(type, packet);
  }

  private clearHandlers() {
    this.#ws.onclose = null;
    this.#ws.onmessage = null;
    this.#ws.onerror = null;
    this.#ws.onopen = null;
  }

  private addHandlers() {
    this.#ws.onmessage = this.onMessageHandler.bind(this);
    this.#ws.onclose = this.onCloseHandler.bind(this);
    this.#ws.onopen = this.onOpenHandler.bind(this);
    this.#ws.onerror = this.onErrorHandler.bind(this);
  }

  private onCloseHandler(e: CloseEvent) {
    this.clearHandlers();
    this.#ws = null;
    this.invoke('close', e);
    if (!this.#disconnecting) {
      let cancelMaybe: CancelHolder = {
        cancel: false,
      };

      let timeout = (((this.#connectionAttempts ** 1.5) / 2 >> 0) * 1000 + (Math.random() * 500)) >> 0;
      this.invoke('timeout', this.#connectionAttempts, timeout, cancelMaybe);

      if (cancelMaybe.cancel === true) {
        console.warn('[WebSocket] A `reconnect` listener flagged `cancel`, no further automatic reconnection attempts will be made.');
        this.#disconnecting = true;
        return;
      }

      this.#connectionAttempts++;
      // Slight exponential backoff with random jitter between [0ms, 500ms)
      setTimeout(() => {
        this.invoke('reconnect', this.#connectionAttempts);
        this.connect();
      }, timeout);
    }
  }

  private onOpenHandler(e: Event) {
    this.#connectionAttempts = 0;
    while (this.#sendBuff.length > 0) {
      if (!this.send(this.#sendBuff.shift())) break;
    }

    this.invoke('open', e);
  }

  private onMessageHandler(e: MessageEvent) {
    this.invoke('message', e);

    if (e && typeof e.data === 'string' && e.data.trim().length > 0) {
      let parsed = undefined;
      try {
        parsed = JSON.parse(e.data);
      } catch (e) {
        //
      }

      if (parsed !== undefined && parsed.type) {
        this.fire(parsed.type, parsed);
      }
    }
  }

  private onErrorHandler(e: Event) {
    this.invoke('error', e);
  }

}

export default WS;
