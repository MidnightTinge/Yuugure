import clsx from 'clsx';
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
            <div className="mb-2">
              <DeleteAction upload={upload} onDeleteInitiated={onDeleteInitiated}/>
            </div>
            <div>
              <PrivateAction upload={upload} checked={upload.state.PRIVATE}/>
            </div>
          </>
        ) : null}
      </section>
      {authed && account.hasModPerms ? (
        <>
          {/* if the user has admin perms we stack mod+admin side-by-side, hence stretching with col-span-2 if they don't have the perms. */}
          <section className={clsx('bg-gray-100 border border-gray-200 rounded-sm shadow p-3 inline-block m-2', (!account.hasAdminPerms) && 'col-span-2')}>
            <p className="text-xl">Moderator Actions</p>
            <hr className="mb-2"/>
            <div className="mb-2">
              <DeleteAction upload={upload} onDeleteInitiated={onDeleteInitiated} addReason/>
            </div>
            <div className="mb-2">
              <p className="text-gray-300">Placeholder (modqueue actions)</p>
            </div>
          </section>
          {account.hasAdminPerms ? (
            <section className="bg-gray-100 border border-gray-200 rounded-sm shadow p-3 inline-block m-2">
              <p className="text-xl">Admin Actions</p>
              <hr className="mb-2"/>
              <div>
                <PrivateAction upload={upload} checked={upload.state.PRIVATE}/>
              </div>
              <div className="mb-2">
                <p className="text-gray-300">Placeholder (reprocess, block media, etc)</p>
              </div>
            </section>
          ) : null}
        </>
      ) : null}
    </div>
  );
}
