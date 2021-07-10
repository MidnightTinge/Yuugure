import clsx from 'clsx';
import * as React from 'react';

export type ModalFooterProps = React.HTMLProps<HTMLDivElement> & {
  children?: React.ReactFragment;
};

export default function ModalFooter(props: ModalFooterProps) {
  let {children, className, ...elProps} = props;

  return (
    <div className={clsx('ModalFooter', className)} children={children} {...elProps}/>
  );
}
