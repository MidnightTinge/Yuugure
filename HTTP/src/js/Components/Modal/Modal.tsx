/*! Animation helpers pulled from https://stackoverflow.com/a/54114180 */

import * as React from 'react';
import {useState} from 'react';
import {createPortal} from 'react-dom';
import Util from '../../classes/Util';
import useDelayUnmount from '../../Hooks/useDelayUnmount';
import ModalBackdrop from './ModalBackdrop';

export enum CloseSource {
  MOUSE,
  KEYBOARD,
  HEADER,
}

const ANIMATION_LENGTH = 100;

function noop() {
  //
}

export type ModalProps = React.HTMLProps<HTMLDivElement> & {
  show: boolean;
  children: React.ReactFragment;
  onCloseRequest: (cs: CloseSource) => any;
  closeButton?: boolean;
  onOpen?: () => void;
  onClose?: () => void;
};

export default function Modal(props: ModalProps) {
  let {show, children, onCloseRequest, className, closeButton = false, onOpen = noop, onClose = noop, ...elProps} = props;

  const render = useDelayUnmount(show, ANIMATION_LENGTH + 1);
  const [style, setStyle] = useState<React.CSSProperties>({});

  const animation = show ? `model-mount ${ANIMATION_LENGTH}ms ease-in` : `model-unmount ${ANIMATION_LENGTH}ms ease-out`;

  function handleAnimationEnd(e: React.AnimationEvent) {
    // ensure this component triggered the event rather than a child
    if (e.currentTarget === e.target) {
      // set final state for animation. when rendering, we've faded out.
      setStyle({
        opacity: show ? 1 : 0,
      });
      if (show) {
        onOpen();
      } else {
        onClose();
      }
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

  return render ? (
    createPortal((
      <ModalBackdrop onBackdropCloseRequest={onCloseRequest} style={{...style, animation}} onAnimationStart={handleAnimationStart} onAnimationEnd={handleAnimationEnd}>
        <div className={Util.joinedClassName('Modal', className)} role="dialog" aria-modal="true" {...elProps}>
          {closeButton ? (<button className="CloseButton" onClick={() => onCloseRequest(CloseSource.HEADER)}><i className="fas fa-times" aria-hidden="true"/> <span className="sr-only">Close</span></button>) : null}
          {children}
        </div>
      </ModalBackdrop>
    ), document.body)
  ) : null;
}
