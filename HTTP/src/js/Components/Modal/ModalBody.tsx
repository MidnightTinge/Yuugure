import clsx from 'clsx';
import * as React from 'react';

export type ModalBodyProps = React.HTMLProps<HTMLDivElement> & {
  children: React.ReactFragment
};

export default function ModalBody(props: ModalBodyProps) {
  let {children, className, ...elProps} = props;

  return (
    <div className={clsx('ModalBody', className)} children={children} {...elProps}/>
  );
}
