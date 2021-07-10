import clsx from 'clsx';
import * as React from 'react';
import {CloseSource} from './Modal';
import ModalContext from './ModalContext';

export type ModalHeaderProps = React.HTMLProps<HTMLDivElement> & {
  children?: React.ReactFragment;
};

export default function ModalHeader(props: ModalHeaderProps) {
  let {children, className, ...elProps} = (props as any);

  return (
    <ModalContext.Consumer>{ctx => (
      <div className={clsx('ModalHeader', className)} {...elProps}>
        {ctx.closeButton ? (
          <button className="CloseButton" onClick={() => ctx.onCloseRequest(CloseSource.HEADER)}><i className="fas fa-times" aria-hidden="true"/> <span className="sr-only">Close</span></button>
        ) : null}
        {children}
      </div>
    )}</ModalContext.Consumer>
  );
}
