import {createContext} from 'react';

export default function namedContext<T>(name: string, props: T) {
  const ret = createContext(props);
  ret.displayName = name;

  return ret;
}
