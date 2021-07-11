import {mdiHelpCircle} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import {useContext, useMemo} from 'react';
import Util from '../../classes/Util';
import DetailsBox from '../../Components/DetailsBox';
import {PageViewContext} from './PageView';

type ViewerDetails = {
  uid: number;
  mid: number;
  score: number;
  bookmarks: number;
  filesize: number;
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
      filesize: -1,
    };

    if (upload != null) {
      ret = {
        uid: upload.upload.id,
        mid: upload.media.id,
        score: upload.votes.total_upvotes - upload.votes.total_downvotes,
        bookmarks: upload.bookmarks.total_public,
        filesize: upload.media_meta.filesize,
      };
    }

    return ret;
  }, [upload?.upload, upload?.media, upload?.votes, upload?.bookmarks, upload?.media_meta]);

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
            <th className="text-right pr-3">Bookmarks:</th>
            <td className="relative">
              {details.bookmarks}
              <span className="block absolute text-xs top-0 transform translate-x-2.5 -translate-y-1 text-gray-400 hover:text-gray-600 underline" title="This number only counts public bookmarks.">
                <Icon path={mdiHelpCircle} size="0.75rem" className="relative bottom-px inline-block"/>
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      {details.uid > 0 ? (
        <div className="mt-3">
          <p className="text-gray-600 font-medium" title={`${Intl.NumberFormat().format(details.filesize)} Bytes`}>Filesize: {Util.formatBytes(details.filesize)}</p>
          <p><a href={`/full/${details.uid}`} className="underline text-blue-500 hover:text-blue-600 focus:outline-none">Direct Link</a></p>
        </div>
      ) : null}
    </DetailsBox>
  );
}
