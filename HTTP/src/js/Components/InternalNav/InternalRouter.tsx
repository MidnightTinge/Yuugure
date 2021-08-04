import * as React from 'react';
import {useLocation} from 'react-router';

import InternalNavContext from './InternalNavContext';
import {InternalRouteProps} from './InternalRoute';

export type InternalRouterProps = {
  children: Arrayable<React.ReactElement<InternalRouteProps>>,
  defaultPath?: string;
};

export const InternalRouter: React.FC<InternalRouterProps> = ({children, defaultPath}: InternalRouterProps) => {
  const loc = useLocation();

  let path: string = defaultPath;
  if (loc.search) {
    // if our search has any KVPs, we want to filter those out and just get the first key.
    path = (loc.search.startsWith('?') ? loc.search.substring(1) : loc.search)
      .split('&')
      .map(kvp => kvp.split('=')[0])[0];
  }

  return (
    <InternalNavContext.Provider value={{path}} children={children}/>
  );
};

export default InternalRouter;
