import * as React from 'react';
import {Container, create} from 'react-modal-promise';
import {InstanceProps} from 'react-modal-promise/lib/types';
import Button from '../../../Components/Button';
import {makeDangerousPasswordConfirmation, ResourceDeletion} from './DangerousActionHelper';

export type DeleteAccountProps = {
  //
};

export default function DeleteAccount(props: DeleteAccountProps) {
  // WARNING: This is a destructive element - it initiates deletion on mount.
  const DeletionStatusModal = ({isOpen, onResolve, confirmationToken}: InstanceProps<any> & { confirmationToken: string }) => {
    return (
      <ResourceDeletion endpoint="/api/account/@me" resourceDescriptor="Account" confirmationToken={confirmationToken} onResolve={onResolve} show={isOpen}/>
    );
  };

  function handleAction() {
    makeDangerousPasswordConfirmation({
      warningText: 'Your account will be deleted, your bookmarks and votes will be removed from uploads, and your uploads will be marked as deleted. There is no way to undo this action.',
      confirmPhrase: 'delete account',
    }).then((state) => {
      if (state.authed === true) {
        create(DeletionStatusModal)({confirmationToken: state.confirmToken}).then(() => {
          (window as any).location = '/';
        });
      }
    });
  }

  return (
    <>
      <Container/>
      <div>
        <Button variant="red" onClick={handleAction} link>Delete Account</Button>
      </div>
    </>
  );
}
