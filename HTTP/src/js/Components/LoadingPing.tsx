import * as React from 'react';
import Util from '../classes/Util';

export type LoadingPingProps = {
  icon?: string;
};

export default function LoadingPing(props: LoadingPingProps) {
  return (
    <>
      <i className={Util.joinedClassName('text-gray-400 fa-5x animate-ping', props.icon || 'fas fa-hourglass-half')} aria-hidden={true}/><span className="sr-only">Loading...</span>
    </>
  );
}
