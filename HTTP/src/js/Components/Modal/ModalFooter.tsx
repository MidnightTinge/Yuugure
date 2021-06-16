import * as React from 'react';
import Util from '../../classes/Util';

export type ModalFooterProps = React.HTMLProps<HTMLDivElement> & {
  children?: React.ReactFragment;
};

export default function ModalFooter(props: ModalFooterProps) {
  let {children, className, ...elProps} = props;

  return (
    <div className={Util.joinedClassName('ModalFooter', className)} children={children} {...elProps}/>
  );
}
