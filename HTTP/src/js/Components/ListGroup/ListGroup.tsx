import * as React from 'react';
import Util from '../../classes/Util';

export type ListGroupProps = React.HTMLAttributes<{
  children: React.ReactFragment
}>;

export default function ListGroup(props: ListGroupProps) {
  const {children, className, ...elProps} = props;

  return (
    <div className={Util.joinedClassName('ListGroup', className)} {...elProps}>
      {children}
    </div>
  );
}
