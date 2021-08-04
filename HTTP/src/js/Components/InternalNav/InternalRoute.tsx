import * as React from 'react';
import InternalNavContext from './InternalNavContext';

export type InternalRouteProps = {
  path: string;
  children: React.ReactFragment;
  exact?: true;
}

export const InternalRoute: React.FC<InternalRouteProps> = (props: InternalRouteProps) => {
  return (
    <InternalNavContext.Consumer>
      {ctx => (
        checkPath(props, ctx.path) ? props.children : null
      )}
    </InternalNavContext.Consumer>
  );
};
export default InternalRoute;

export function checkPath(props: InternalRouteProps, path: string): boolean {
  if (props.path === '*') {
    return true;
  }

  if (path == null || !path.trim()) {
    return false;
  }

  return props.exact === true
    ? props.path === path
    : props.path.toLowerCase() === path.toLowerCase();
}
