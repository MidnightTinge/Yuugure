import * as React from 'react';

import InternalNavContext from './InternalNavContext';
import {checkPath, InternalRouteProps} from './InternalRoute';

export type InternalSwitchProps = {
  children: Arrayable<React.ReactElement<InternalRouteProps>>
};

export const InternalSwitch: React.FC<InternalSwitchProps> = (props: InternalSwitchProps) => {
  return (
    <InternalNavContext.Consumer>
      {context => {
        let match: React.ReactElement<InternalRouteProps> = null;

        React.Children.forEach(props.children, child => {
          if (match == null && React.isValidElement(child)) {
            if (checkPath(child.props, context.path)) {
              match = child;
            }
          }
        });

        return match != null ? React.cloneElement(match) : null;
      }}
    </InternalNavContext.Consumer>
  );
};

export default InternalSwitch;
