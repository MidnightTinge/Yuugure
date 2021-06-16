import * as React from 'react';
import Util from '../../classes/Util';

export type ModalBodyProps = React.HTMLProps<HTMLDivElement> & {
  children: React.ReactFragment
};

export default function ModalBody(props: ModalBodyProps) {
  let {children, className, ...elProps} = props;

  return (
    <div className={Util.joinedClassName('ModalBody', className)} children={children} {...elProps}/>
  );
}
