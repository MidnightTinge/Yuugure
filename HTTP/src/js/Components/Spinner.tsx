import {mdiLoading} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';

export type SpinnerProps = {
  size?: number | string;
  spin?: number | boolean;
  className?: string;
};

export default function Spinner({size = 1, className, spin = true}: SpinnerProps) {
  return (
    <Icon path={mdiLoading} size={size} className={className} spin={spin}/>
  );
}
