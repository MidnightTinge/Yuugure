import {mdiLoading} from '@mdi/js';
import Icon from '@mdi/react';
import clsx from 'clsx';
import * as React from 'react';

export type SpinnerProps = {
  size?: number | string;
  spin?: number | boolean;
  inline?: boolean;
  className?: string;
};

export const Spinner: React.FC<SpinnerProps> = ({size = 1, className, spin = true, inline = false}: SpinnerProps) => {
  return (
    <Icon path={mdiLoading} size={size} className={clsx(className, inline && 'inline-block mr-px relative bottom-px')} spin={spin}/>
  );
};

export default Spinner;
