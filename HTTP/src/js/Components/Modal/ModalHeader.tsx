import {mdiClose} from '@mdi/js';
import Icon from '@mdi/react';
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
          <button className="CloseButton" onClick={() => ctx.onCloseRequest(CloseSource.HEADER)}><Icon path={mdiClose} size={1} aria-label="Close"/></button>
        ) : null}
        {children}
      </div>
    )}</ModalContext.Consumer>
  );
}
