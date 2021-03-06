import {mdiBookmark, mdiChevronDown, mdiChevronUp} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import BookmarkPopup from './BookmarkPopup';

export type ActionType = 'bookmark' | 'vote';
export type ActionArgs = { public?: boolean, upvote?: boolean, remove: boolean } | { public: boolean, upvote?: never, remove: boolean } | { public?: never, upvote: boolean, remove: boolean };
export type Action = {
  type: ActionType,
  args: ActionArgs,
}
export type ActionHandler = (action: Action) => void;

export type BookmarkState = {
  bookmark: {
    active: boolean;
    isPrivate: boolean;
  };
  upvote: boolean;
  downvote: boolean;
};

type BookmarkFavBarProps = {
  resourceType: string;
  resourceId: number;
  bookmarkState: BookmarkState;
  onAction?: ActionHandler;
};

type _State = {
  posting: boolean;
  error: string;
};

export default class BookmarkFavBar extends React.Component<BookmarkFavBarProps, _State> {
  constructor(props: BookmarkFavBarProps) {
    super(props);
    this.state = {
      posting: false,
      error: null,
    };
  }

  _doUpvote(): void {
    this._doAction({
      type: 'vote',
      args: {
        upvote: true,
        // remove the vote entirely if we've voted and our current state is an upvote
        remove: this.props.bookmarkState.upvote,
      },
    });
  }

  _doDownvote(): void {
    this._doAction({
      type: 'vote',
      args: {
        upvote: false,
        // remove the vote entirely if our current state is a downvote
        remove: this.props.bookmarkState.downvote,
      },
    });
  }

  _doAction(action: Action): void {
    if (typeof this.props.onAction === 'function') {
      this.props.onAction(action);
    }
  }

  // eslint-disable-next-line
  render() {
    const bookmarkColor = this.props.bookmarkState.bookmark.active ? 'text-pink-500' : 'text-blue-500';
    const upvoteColor = this.props.bookmarkState.upvote ? 'text-green-500' : 'text-blue-500';
    const downvoteColor = this.props.bookmarkState.downvote ? 'text-red-500' : 'text-blue-500';

    return (
      <div className="flex flex-row rounded w-full">
        <button onClick={this._doUpvote.bind(this)} className={`px-2 flex-grow ${upvoteColor} rounded-l border border-r-0 border-blue-200 bg-blue-100 hover:bg-blue-200 focus:outline-none focus-within:outline-none`}>
          <Icon path={mdiChevronUp} size={1} className="relative bottom-px inline-block"/><span className="sr-only">Upvote</span>
        </button>
        <BookmarkPopup bookmarkState={{...this.props.bookmarkState}} onAction={this._doAction.bind(this)} className={`px-2 flex-grow ${bookmarkColor} border border-r-0 border-blue-200 bg-blue-100 hover:bg-blue-200 focus:outline-none focus-within:outline-none`}>
          <Icon path={mdiBookmark} size={1} className="relative bottom-px inline-block"/><span className="sr-only">Bookmark</span>
        </BookmarkPopup>
        <button onClick={this._doDownvote.bind(this)} className={`px-2 flex-grow ${downvoteColor} rounded-r border border-r-0 border-blue-200 bg-blue-100 hover:bg-blue-200 focus:outline-none focus-within:outline-none`}>
          <Icon path={mdiChevronDown} size={1} className="relative bottom-px inline-block"/><span className="sr-only">Downvote</span>
        </button>
      </div>
    );
  }
}
