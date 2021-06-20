import * as React from 'react';
import Util from '../classes/Util';

export type SpinnerProps = {
  method?: 'spin' | 'pulse';
  size?: number;
  className?: string;
};

export default function Spinner({method = 'pulse', size = 1, className}: SpinnerProps) {
  return (
    <i className={Util.joinedClassName(`fas fa-spinner fa-${method} fa-${size}x`, className)}/>
  );
}
