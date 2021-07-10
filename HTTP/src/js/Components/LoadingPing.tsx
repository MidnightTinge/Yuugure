import clsx from 'clsx';
import * as React from 'react';

export type LoadingPingProps = {
  icon?: string;
};

export default function LoadingPing(props: LoadingPingProps) {
  return (
    <>
      <i className={clsx('text-gray-400 fa-5x animate-ping', props.icon || 'fas fa-hourglass-half')} aria-hidden={true}/><span className="sr-only">Loading...</span>
    </>
  );
}
