import clsx from 'clsx';
import * as React from 'react';
import {useEffect, useState} from 'react';
import {createPortal} from 'react-dom';
import {usePopper} from 'react-popper';
import {ActionHandler, BookmarkState} from './BookmarkFavBar';

export type BookmarkPopupProps = {
  bookmarkState: BookmarkState;
  onAction?: ActionHandler;
  className: string;
  children: React.ReactFragment;
};

export const BookmarkPopup: React.FC<BookmarkPopupProps> = (props: BookmarkPopupProps) => {
  const [referenceElement, setReferenceElement] = React.useState(null);
  const [popperElement, setPopperElement] = React.useState(null);
  const {styles, attributes} = usePopper(referenceElement, popperElement);
  const [show, setShow] = useState(false);

  useEffect(function mounted() {
    function handleWindowClick(e: PointerEvent) {
      if (popperElement == null) return;
      const target = e.target as HTMLElement;

      if (target.closest('.BookmarksPopupTrigger') != null) return;
      setShow(target.closest('.BookmarksPopup') != null);
    }

    window.addEventListener('pointerup', handleWindowClick);

    return function unmounted() {
      window.removeEventListener('pointerup', handleWindowClick);
    };
  });

  function toggleShow() {
    setShow(!show);
  }

  function handlePublicClick() {
    if (typeof props.onAction === 'function') {
      props.onAction({
        type: 'bookmark',
        args: {
          public: true,
          remove: props.bookmarkState.bookmark.active && !props.bookmarkState.bookmark.isPrivate,
        },
      });
    }
  }

  function handlePrivateClick() {
    if (typeof props.onAction === 'function') {
      props.onAction({
        type: 'bookmark',
        args: {
          public: false,
          remove: props.bookmarkState.bookmark.active && props.bookmarkState.bookmark.isPrivate,
        },
      });
    }
  }

  return (
    <>
      <button type="button" className={clsx('BookmarksPopupTrigger', props.className)} ref={setReferenceElement} onClick={toggleShow}>{props.children}</button>
      {show ? createPortal((
        <div ref={setPopperElement} style={{...styles.popper}} {...attributes.popper} className="BookmarksPopup flex flex-col">
          <button onClick={handlePublicClick} className={`flex-grow px-3 py-1 ${props.bookmarkState.bookmark.active && !props.bookmarkState.bookmark.isPrivate ? 'text-gray-800 font-bold' : ' text-gray-600 font-medium'} bg-blue-200 border border-b-0 border-blue-300 hover:bg-blue-300 rounded-t-md focus:outline-none focus-within:outline-none`}>Publicly</button>
          <button onClick={handlePrivateClick} className={`flex-grow px-3 py-1 ${props.bookmarkState.bookmark.active && props.bookmarkState.bookmark.isPrivate ? 'text-gray-800 font-bold' : ' text-gray-600 font-medium'} bg-blue-200 border            border-blue-300 hover:bg-blue-300 rounded-b-md focus:outline-none focus-within:outline-none`}>Privately</button>
        </div>
      ), document.body) : null}
    </>
  );
};

export default BookmarkPopup;
