/* ! Animation helpers pulled from https://stackoverflow.com/a/54114180 */

import clsx from 'clsx';
import * as React from 'react';
import {useEffect, useState} from 'react';
import {createPortal} from 'react-dom';
import useDelayUnmount from '../../Hooks/useDelayUnmount';
import ModalBackdrop from './ModalBackdrop';
import ModalBody, {ModalBodyProps} from './ModalBody';
import ModalContext from './ModalContext';
import ModalFooter, {ModalFooterProps} from './ModalFooter';
import ModalHeader, {ModalHeaderProps} from './ModalHeader';

export enum CloseSource {
  MOUSE,
  KEYBOARD,
  HEADER,
}

const ANIMATION_LENGTH = 100;

function noop() {
  //
}

interface IModalComposition {
  Header: React.FC<ModalHeaderProps>,
  Body: React.FC<ModalBodyProps>,
  Footer: React.FC<ModalFooterProps>,
}

export type ModalProps = React.HTMLProps<HTMLDivElement> & {
  show: boolean;
  children: React.ReactFragment;
  onCloseRequest?: (cs: CloseSource) => any;
  closeButton?: boolean;
  onOpen?: () => void;
  onClose?: () => void;
};

const Modal: React.FC<ModalProps> & IModalComposition = (props: ModalProps) => {
  const {show, children, onCloseRequest, className, closeButton = false, onOpen = noop, onClose = noop, ...elProps} = props;

  const render = useDelayUnmount(show, ANIMATION_LENGTH + 1);
  const [style, setStyle] = useState<React.CSSProperties>({});

  const animation = show ? `model-mount ${ANIMATION_LENGTH}ms ease-in` : `model-unmount ${ANIMATION_LENGTH}ms ease-out`;

  // Effect is the only reliable method that is fired to show open/close state change. This fixes
  // onOpen/onClose not firing seemingly randomly (CU-ykbecg).
  useEffect(() => {
    if (show) {
      onOpen();
    } else {
      onClose();
    }
  }, [render]);

  function handleAnimationEnd(e: React.AnimationEvent) {
    // ensure this component triggered the event rather than a child
    if (e.currentTarget === e.target) {
      // set final state for animation. when rendering, we've faded out.
      setStyle({
        opacity: show ? 1 : 0,
      });
    }
  }

  function handleAnimationStart(e: React.AnimationEvent) {
    // ensure this component triggered the event rather than a child
    if (e.currentTarget === e.target) {
      // set initial state for animation. when rendering, we're fading in.
      setStyle({
        opacity: show ? 0 : 1,
      });
    }
  }

  const closeRequestHandler = typeof onCloseRequest === 'function' ? onCloseRequest : ((e: CloseSource) => e);

  return render ? (
    createPortal((
      <ModalContext.Provider value={{closeButton: closeButton === true, onCloseRequest: closeRequestHandler}}>
        <ModalBackdrop onBackdropCloseRequest={closeRequestHandler} style={{...style, animation}} onAnimationStart={handleAnimationStart} onAnimationEnd={handleAnimationEnd}>
          <div className={clsx('Modal', className)} role="dialog" aria-modal="true" {...elProps}>
            {children}
          </div>
        </ModalBackdrop>
      </ModalContext.Provider>
    ), document.body)
  ) : null;
};
Modal.Header = ModalHeader;
Modal.Body = ModalBody;
Modal.Footer = ModalFooter;

export default Modal;
