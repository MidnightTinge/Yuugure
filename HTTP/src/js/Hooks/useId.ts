import {useMemo} from 'react';
import * as React from 'react';
import Util from '../classes/Util';

export default function useId() {
  const id = useMemo(() => Util.mkid(), []);

  return id;
}
