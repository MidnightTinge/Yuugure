import * as React from 'react';
import DeleteAccount from './actions/DeleteAccount';
import DeleteUploads from './actions/DeleteUploads';
import UpdateEmail from './actions/UpdateEmail';
import UpdatePassword from './actions/UpdatePassword';

export const AccountSettingsContext = React.createContext<{ account: SafeAccount }>({account: null});
AccountSettingsContext.displayName = 'AccountSettingsContext';

export type AccountSettingsProps = {
  account: SafeAccount;
};

export const AccountSettings: React.FC<AccountSettingsProps> = ({account}: AccountSettingsProps) => {
  return (
    <AccountSettingsContext.Provider value={{account}}>
      <div className="grid grid-cols-2">
        <section className="bg-gray-100 border border-gray-200 rounded-sm shadow p-3 inline-block m-2">
          <p className="text-xl">Account Settings</p>
          <hr className="mb-1"/>
          <UpdateEmail/>
          <UpdatePassword/>
          <DeleteAccount/>
        </section>
        <section className="bg-gray-100 border border-gray-200 rounded-sm shadow p-3 inline-block m-2">
          <p className="text-xl">Upload Settings</p>
          <hr className="mb-1"/>
          <DeleteUploads/>
        </section>
        <section className="bg-gray-100 border border-gray-200 rounded-sm shadow p-3 inline-block m-2 col-span-2">
          <p className="text-xl">Site Settings</p>
          <hr/>
          <p className="text-gray-400 italic text-center">Placeholder (tag blacklist, downvote threshold, default upload tags, default upload rating)</p>
        </section>
      </div>
    </AccountSettingsContext.Provider>
  );
};

export default AccountSettings;
