import * as React from 'react';
import {useAuthState} from '../../../Context/AuthStateProvider';
import DeleteAction from './DeleteAction';
import PrivateAction from './PrivateAction';
import ReportAction from './ReportAction';

export type ActionProviderProps = {
  upload: RenderableUpload;
  onDeleteInitiated: () => void;
};

export default function ActionProvider({upload, onDeleteInitiated}: ActionProviderProps) {
  const {authed, account} = useAuthState();

  return (
    <div className="grid grid-cols-2">
      <section className="bg-gray-100 border border-gray-200 rounded-sm shadow p-3 inline-block m-2 col-span-2">
        <p className="text-xl">Upload Actions</p>
        <hr className="mb-2"/>
        <div className="mb-2">
          <ReportAction upload={upload}/>
        </div>
        {authed && account.id === upload.owner.id ? (
          <>
            {/* TODO: Show delete to moderators */}
            <div className="mb-2">
              <DeleteAction upload={upload} onDeleteInitiated={onDeleteInitiated}/>
            </div>
            <div>
              <PrivateAction upload={upload} checked={upload.state.PRIVATE}/>
            </div>
            {/* TODO: Show actions to admins (reprocess, block media, etc) */}
            {/* TODO: Show actions to moderators (modqueue) */}
          </>
        ) : null}
      </section>
    </div>
  );
}
