import * as React from 'react';
import {useContext, useMemo} from 'react';
import {AutoSizer, CellMeasurer, CellMeasurerCache, List, ListRowProps} from 'react-virtualized';
import Comment from '../../../Components/Comment';
import NewCommentBlock from '../../../Components/modals/NewCommentBlock';
import {useAuthState} from '../../../Context/AuthStateProvider';
import {PageViewContext} from '../PageView';

export type ViewerCommentsProps = {
  //
};

export default function ViewerComments(props: ViewerCommentsProps) {
  const authState = useAuthState();
  const {comments} = useContext(PageViewContext);

  const cellMeasurerCache = useMemo(() => {
    return new CellMeasurerCache({
      defaultHeight: 50,
      fixedWidth: true,
      keyMapper: (row) => {
        return comments.comments[row].id;
      },
    });
  }, [comments.comments]);

  function commentRowRenderer({key, index, style, parent}: ListRowProps): React.ReactNode {
    return (
      <CellMeasurer
        cache={cellMeasurerCache}
        columnIndex={0}
        key={key}
        rowIndex={index}
        parent={parent}>
        {({registerChild}) => (
          <div ref={registerChild} style={style} className="px-2 py-0.5">
            <Comment comment={comments.comments[index]}/>
          </div>
        )}
      </CellMeasurer>
    );
  }

  return (
    <PageViewContext.Consumer>
      {({upload, comments}) => (
        <div className="flex flex-col h-full">
          {authState.authed || comments.error ? (
            <div className="bg-gray-100 border border-gray-200 rounded-sm shadow px-3 py-1 mb-2 flex-grow-0 flex-shrink">
              {comments.error ? (<p className="text-red-500 text-lg whitespace-pre-wrap">{comments.error}</p>) : null}
              {authState.authed ? (
                authState.account.state.COMMENTS_RESTRICTED ? (
                  <p className="text-center text-red-500 text-lg">You are restricted from creating new comments.</p>
                ) : (
                  <NewCommentBlock targetType="upload" targetId={upload.upload.id}/>
                )
              ) : null}
            </div>
          ) : null}
          <div className="flex-grow flex-shrink-0">
            <AutoSizer>
              {({width, height}) => (
                <List
                  width={width}
                  height={height}
                  deferredMeasurementCache={cellMeasurerCache}
                  overscanRowCount={0}
                  rowCount={comments.comments.length}
                  rowHeight={({index}) => cellMeasurerCache.getHeight(index, 0)}
                  rowRenderer={commentRowRenderer}
                />
              )}
            </AutoSizer>
          </div>
        </div>
      )}
    </PageViewContext.Consumer>
  );
}
