import clsx from 'clsx';
import * as React from 'react';

export type ModalBodyProps = React.HTMLProps<HTMLDivElement> & {
  children: React.ReactFragment
};

export const ModalBody: React.FC<ModalBodyProps> = (props: ModalBodyProps) => {
  const {children, className, ...elProps} = props;

  return (
    <div className={clsx('ModalBody', className)} children={children} {...elProps}/>
  );
};

export default ModalBody;
