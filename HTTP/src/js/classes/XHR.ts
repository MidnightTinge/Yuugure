import RouterResponseConsumer, {ConsumedRouterResponse} from './RouterResponseConsumer';

enum BODY_TYPE {
  JSON = 'JSON',
  RAW = 'RAW',
  FORM = 'FORM',
  FORM_DATA = 'FORM_DATA',
  NONE = 'NONE',
}

const _body_type_map: Readonly<{ [k: string]: string }> = Object.freeze({
  'JSON': 'application/json',
  'FORM': 'application/x-www-form-urlencoded',
  'FORM_DATA': null, // IMPORTANT: If you explicitly set `Content-Type` during XHR build, the browser will be unable to specify the boundary and will send a malformed header.
});

export class XHR {
  readonly #url: string;

  constructor(url: string) {
    this.#url = url;
  }

  static for(url: string) {
    return new XHR(url);
  }

  get() {
    return new XHRFetcher(this.#url, 'GET', BODY_TYPE.NONE, null);
  }

  post(bodyType: BODY_TYPE = BODY_TYPE.NONE, body: any = null) {
    if (typeof bodyType !== 'string') throw new TypeError('Fatal: Invalid bodyType supplied. Expected a string but got ' + typeof bodyType + ' instead.');
    return new XHRFetcher(this.#url, 'POST', bodyType, body);
  }

  delete(bodyType: BODY_TYPE = BODY_TYPE.NONE, body: any = null) {
    if (typeof bodyType !== 'string') throw new TypeError('Fatal: Invalid bodyType supplied. Expected a string but got ' + typeof bodyType + ' instead.');
    return new XHRFetcher(this.#url, 'DELETE', bodyType, body);
  }

  patch(bodyType: BODY_TYPE = BODY_TYPE.NONE, body: any = null) {
    if (typeof bodyType !== 'string') throw new TypeError('Fatal: Invalid bodyType supplied. Expected a string but got ' + typeof bodyType + ' instead.');
    return new XHRFetcher(this.#url, 'PATCH', bodyType, body);
  }

  put(bodyType: BODY_TYPE = BODY_TYPE.NONE, body: any = null) {
    if (typeof bodyType !== 'string') throw new TypeError('Fatal: Invalid bodyType supplied. Expected a string but got ' + typeof bodyType + ' instead.');
    return new XHRFetcher(this.#url, 'PUT', bodyType, body);
  }

  custom(method: string, bodyType: BODY_TYPE = BODY_TYPE.NONE, body: any = null) {
    if (typeof bodyType !== 'string') throw new TypeError('Fatal: Invalid bodyType supplied. Expected a string but got ' + typeof bodyType + ' instead.');
    return new XHRFetcher(this.#url, method, bodyType, body);
  }

  static BODY_TYPE = BODY_TYPE;
}

export class XHRFetcher {
  #req: XMLHttpRequest;
  readonly #url: string;
  readonly #body: any = null;
  #fired: boolean = false;

  constructor(url: string, method: string, bodyType: BODY_TYPE, bodyData: any = null) {
    // These are here in an attempt to help upstream code catch issues faster. There's no reason to
    // supply a body when `bodyType` is `none`, so if someone forgot to reset the bodyType or
    // something we want to point it out.
    if (bodyType === BODY_TYPE.NONE && bodyData != null) throw new TypeError(`Fatal: Did not expect "bodyData" because "bodyType" was not set to "NONE"`);
    if (bodyType !== BODY_TYPE.NONE && bodyData == null) throw new TypeError(`Fatal: Expected non-null "bodyData" because "bodyType" has been set to something other than "NONE"`);

    this.#url = url;
    this.#body = bodyData;
    this.#req = new XMLHttpRequest();

    this.#req.open(method.toUpperCase(), this.#url);

    const contentType = _body_type_map[bodyType] || false;
    if (contentType) {
      this.#req.setRequestHeader('Content-Type', contentType);
    }

    switch (bodyType) {
      case BODY_TYPE.JSON: {
        if (typeof this.#body !== 'string') {
          this.#body = JSON.stringify(this.#body);
        }
        break;
      }
      case BODY_TYPE.FORM: {
        if (typeof this.#body !== 'string') {
          this.#body = objToData(this.#body);
        }
        break;
      }
      case BODY_TYPE.FORM_DATA: {
        if (!(this.#body instanceof FormData)) {
          throw new Error('[XHR] Invalid FormData provided.');
        }
      }
    }
  }

  async getJson<T = any>(): Promise<T> {
    if (this.#fired) throw new Error(`This request has already been fired.`);
    return new Promise((resolve, reject) => {
      this.#req.setRequestHeader('Accept', 'application/json;q=1.0, */*;q=0.5');
      try {
        prepareRequest(this.#req, () => reject(), function() {
          try {
            resolve(JSON.parse(this.responseText));
          } catch (e) {
            reject(e);
          }
        }).send(this.#body);
      } catch (e) {
        reject(e);
      }
    });
  }

  async getRouterResponse<T = any>(): Promise<ConsumedRouterResponse<T>> {
    return new Promise((resolve, reject) => {
      this.getJson<RouterResponse<T>>().then(data => {
        resolve(RouterResponseConsumer<T>(data));
      }).catch(err => reject(err));
    });
  }

  async getText(): Promise<any> {
    if (this.#fired) throw new Error(`This request has already been fired.`);
    return new Promise((resolve, reject) => {
      this.#req.setRequestHeader('Accept', 'octet/binary;q=1.0, */*;q=0.5');
      prepareRequest(this.#req, () => reject(), function() {
        resolve(this.responseText);
      }).send(this.#body);
    });
  }

  async getBinary(): Promise<ArrayBuffer> {
    if (this.#fired) throw new Error(`This request has already been fired.`);
    return new Promise((resolve, reject) => {
      this.#req.setRequestHeader('Accept', 'octet/binary;q=1.0, */*;q=0.5');
      this.#req.responseType = 'arraybuffer';
      prepareRequest(this.#req, () => reject(), function() {
        resolve(this.response);
      }).send(this.#body);
    });
  }

}

function prepareRequest(req: XMLHttpRequest, onerror: () => void, onload: () => void) {
  req.onerror = onerror.bind(req);
  req.onload = onload.bind(req);
  return req;
}

function objToData(obj: any) {
  if (typeof obj !== 'object') return '';
  let entries;
  try {
    entries = Object.entries(obj);
  } catch (e) {
    return '';
  }

  let items = [];
  for (let entry of entries) {
    let k = entry[0];
    let v: any = entry[1];

    items.push(`${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
  }

  return items.join('&');
}
