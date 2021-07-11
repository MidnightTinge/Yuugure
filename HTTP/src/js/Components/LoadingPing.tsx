import {mdiTimerSand} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';

export type LoadingPingProps = {
  path?: string;
};

export default function LoadingPing(props: LoadingPingProps) {
  return (
    <>
      <Icon path={props.path || mdiTimerSand} size={5} className="relative animate-ping" aria-label="Loading..."/>
    </>
  );
}
