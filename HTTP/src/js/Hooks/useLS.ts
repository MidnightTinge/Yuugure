import {useEffect, useState} from 'react';

type LocalStorageStateHook<T> = [
  T,
  (t?: T) => void,
];

export default function useLS<T = any>(keyName: string, defaultValue?: T): LocalStorageStateHook<T> {
  const [state, setState] = useState<T>(() => {
    let _value = _get(keyName);
    if (_value == null) {
      if (defaultValue !== undefined) {
        _set(keyName, defaultValue);
      }

      _value = defaultValue;
    }

    return _value;
  });
  useEffect(() => {
    _set(keyName, state);
  }, [state]);

  return [state, setState];
}

function _get(key: string) {
  try {
    return JSON.parse(localStorage.getItem(key));
  } catch (e) {
    // ignored
  }

  return undefined;
}

function _set(key: string, value: any) {
  localStorage.setItem(key, JSON.stringify(value));
}
