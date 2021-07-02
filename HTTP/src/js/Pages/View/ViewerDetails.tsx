import * as React from 'react';
import {useContext, useMemo} from 'react';
import DetailsBox from '../../Components/DetailsBox';
import {PageViewContext} from './PageView';

type ViewerDetails = {
  uid: number;
  mid: number;
  score: number;
  bookmarks: number;
}

export type ViewerDetailsProps = {
  //
};

export default function ViewerDetails(props: ViewerDetailsProps) {
  const {upload} = useContext(PageViewContext);

  const details: ViewerDetails = useMemo(() => {
    let ret: ViewerDetails = {
      uid: -1,
      mid: -1,
      score: 0,
      bookmarks: 0,
    };

    if (upload != null) {
      ret = {
        uid: upload.upload.id,
        mid: upload.media.id,
        score: upload.votes.total_upvotes - upload.votes.total_downvotes,
        bookmarks: upload.bookmarks.total_public,
      };
    }

    return ret;
  }, [upload?.upload, upload?.media, upload?.votes, upload?.bookmarks]);

  return (
    <DetailsBox header="Details">
      <table>
        <tbody>
          <tr>
            <th className="text-right pr-3">ID:</th>
            <td>{details.uid}</td>
          </tr>
          <tr>
            <th className="text-right pr-3">Media:</th>
            <td>{details.mid}</td>
          </tr>
          <tr>
            <th className="text-right pr-3">Score:</th>
            <td className={`${details.score > 0 ? 'text-green-700' : 'text-red-700'}`}>{details.score}</td>
          </tr>
          <tr>
            <th className="text-right pr-3">Public bookmarks:</th>
            <td>{details.bookmarks}</td>
          </tr>
        </tbody>
      </table>
    </DetailsBox>
  );
}
