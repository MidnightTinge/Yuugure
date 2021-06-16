import * as React from 'react';
import Util from '../../classes/Util';

export type ModalHeaderProps = React.HTMLProps<HTMLDivElement> & {
  children?: React.ReactFragment;
};

export default function ModalHeader(props: ModalHeaderProps) {
  let {children, className, ...elProps} = (props as any);

  return (
    <div className={Util.joinedClassName('ModalHeader', className)} {...elProps}>
      {children}
    </div>
  );
}
