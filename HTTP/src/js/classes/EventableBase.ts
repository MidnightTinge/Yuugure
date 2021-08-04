type Handler = (...args: any[]) => any;

type EventMap = {
  [k: string]: Handler[];
}

export class EventableBase {
  readonly _events: EventMap;

  constructor() {
    this._events = new Proxy<EventMap>({}, {
      get: (target, p: string) => {
        if (!Array.isArray(target[p])) {
          target[p] = [];
        }

        return target[p];
      },
    });
  }

  invoke(eventName: string, ...args: any[]): void {
    for (const handler of this._events[eventName]) {
      try {
        handler.apply(window, args);
      } catch (e) {
        console.error(`A handler for the event "${eventName}" threw an error:`, e);
      }
    }
  }

  on(eventName: string, handler: Handler): this {
    return this.addEventHandler(eventName, handler);
  }

  addEventHandler(eventName: string, handler: Handler): this {
    this._events[eventName].push(handler);

    return this;
  }

  removeEventHandler(eventName: string, handler: Handler): boolean {
    for (let i = 0; i < this._events[eventName].length; i++) {
      if (this._events[eventName][i] === handler) {
        this._events[eventName].splice(i, 1);
        return true;
      }
    }

    return false;
  }

}

export default EventableBase;
