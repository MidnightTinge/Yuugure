import * as React from 'react';
import {useEffect} from 'react';
import RelativeTime from '../classes/RelativeTime';

export type PageDebugProps = {
  //
};

export default function PageDebug(props: PageDebugProps) {
  useEffect(() => {
    let then = new Date(1615968346906);
    console.debug(RelativeTime(then));
  }, []);

  return (
    <div/>
  );
}
