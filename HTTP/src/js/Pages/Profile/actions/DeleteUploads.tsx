import * as React from 'react';
import {Container, create} from 'react-modal-promise';
import {InstanceProps} from 'react-modal-promise/lib/types';
import Button from '../../../Components/Button';
import {makeDangerousPasswordConfirmation, ResourceDeletion} from './DangerousActionHelper';

export type DeleteUploadsProps = {
  //
};

export default function DeleteUploads(props: DeleteUploadsProps) {
  // WARNING: This is a destructive element - it initiates deletion on mount.
  const DeletionStatusModal = ({isOpen, onResolve, confirmationToken}: InstanceProps<any> & { confirmationToken: string }) => {
    return (
      <ResourceDeletion endpoint="/api/profile/@me/uploads" resourceDescriptor="Uploads" confirmationToken={confirmationToken} onResolve={onResolve} show={isOpen}/>
    );
  };

  function handleAction() {
    makeDangerousPasswordConfirmation({
      warningText: 'This will remove all of your uploads, both public and private. The only way to reverse this action is for you to manually re-upload each item.',
      confirmPhrase: 'delete uploads',
    }).then((state) => {
      if (state.authed === true) {
        create(DeletionStatusModal)({confirmationToken: state.confirmToken}).then(() => {
          // noop
        });
      }
    });
  }

  return (
    <>
      <Container/>
      <div>
        <Button variant="red" onClick={handleAction} link>Delete Uploads</Button>
      </div>
    </>
  );
}
