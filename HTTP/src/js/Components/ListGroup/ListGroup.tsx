import * as React from 'react';

export type ListGroupProps = React.HTMLAttributes<{
  children: React.ReactFragment
}>;

export default function ListGroup(props: ListGroupProps) {
  const {children, className, ...elProps} = props;

  return (
    <div className={`ListGroup ${className || ''}`} {...elProps}>
      {children}
    </div>
  );
}
