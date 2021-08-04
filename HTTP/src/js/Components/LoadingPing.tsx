import {mdiTimerSand} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';

export type LoadingPingProps = {
  path?: string;
};

export const LoadingPing: React.FC<LoadingPingProps> = (props: LoadingPingProps) => {
  return (
    <>
      <Icon path={props.path || mdiTimerSand} size={5} className="relative animate-ping" aria-label="Loading..."/>
    </>
  );
};

export default LoadingPing;
