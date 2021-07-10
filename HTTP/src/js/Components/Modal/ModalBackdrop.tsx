import clsx from 'clsx';
import * as React from 'react';
import {useEffect} from 'react';
import {CloseSource} from './Modal';

type ModalBackdropCloseRequest = (closeSource: CloseSource) => void;

export type ModalBackdropProps = React.HTMLAttributes<HTMLDivElement> & {
  children: React.ReactElement;
  onBackdropCloseRequest: ModalBackdropCloseRequest;
};

export default function ModalBackdrop(props: ModalBackdropProps) {
  let {onBackdropCloseRequest, children, className, ...elProps} = props;

  // Attach event listeners to the window so that we can trigger a backdrop close request with
  // proper context to support better overall UX, e.g. only send `KEYBOARD` if we don't have an
  // input focus, or only send `MOUSE` when both pointerDown and pointerUp happened on the backdrop.
  // We keep the handlers separate from the addEventListener() so we can remove them on unmount.
  useEffect(function mounted() {
    // Used to keep track of whether or not our initial pointerDown happened inside the modal as
    // opposed to on the backdrop.
    let downWasInside = false;

    function keyUpHandler(e: KeyboardEvent) {
      if (document.querySelector('input:focus') || document.querySelector('textarea:focus')) {
        return;
      }
      if (e.key === 'Escape' || e.code === 'Escape' || e.keyCode === 27) {
        onBackdropCloseRequest(CloseSource.KEYBOARD);
      }
    }

    function pointerUpHandler(e: PointerEvent) {
      if (downWasInside) return;
      if ((e.target as HTMLElement).classList.contains('ModalBackdrop')) {
        onBackdropCloseRequest(CloseSource.MOUSE);
      }
    }

    function pointerDownHandler(e: PointerEvent) {
      downWasInside = !((e.target as HTMLElement).classList.contains('ModalBackdrop'));
    }

    window.addEventListener('keyup', keyUpHandler);
    window.addEventListener('pointerdown', pointerDownHandler);
    window.addEventListener('pointerup', pointerUpHandler);

    return function unmounted() {
      window.removeEventListener('keyup', keyUpHandler);
      window.removeEventListener('pointerdown', pointerDownHandler);
      window.removeEventListener('pointerup', pointerUpHandler);
    };
  });

  return (
    <div className={clsx('ModalBackdrop', className)} children={children} {...elProps}/>
  );
}
