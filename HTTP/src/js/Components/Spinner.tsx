import clsx from 'clsx';
import * as React from 'react';

export type SpinnerProps = {
  method?: 'spin' | 'pulse';
  size?: number;
  className?: string;
};

export default function Spinner({method = 'pulse', size = 1, className}: SpinnerProps) {
  return (
    <i className={clsx(`fas fa-spinner fa-${method} fa-${size}x`, className)}/>
  );
}
