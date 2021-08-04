import {useMemo} from 'react';
import Util from '../classes/Util';

export default function useId(): string {
  return useMemo(() => Util.mkid(), []);
}
