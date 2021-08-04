import {mdiDeleteForever} from '@mdi/js';
import Icon from '@mdi/react';
import {Options} from 'ky';
import * as React from 'react';
import {useEffect, useRef, useState} from 'react';
import {Container, create} from 'react-modal-promise';
import {InstanceProps} from 'react-modal-promise/lib/types';
import KY from '../../../classes/KY';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import Util from '../../../classes/Util';
import {AlertType} from '../../../Components/Alerts/Alert/Alert';
import {useAlerts} from '../../../Components/Alerts/AlertsProvider';
import Button from '../../../Components/Button';
import Modal from '../../../Components/Modal/Modal';
import DangerousConfirmModal from '../../../Components/modals/DangerousConfirmModal';
import InputModal from '../../../Components/modals/InputModal';
import Spinner from '../../../Components/Spinner';

export type DeleteActionProps = {
  upload: RenderableUpload;
  onDeleteInitiated: () => void;
  addReason?: boolean;
};

export const DeleteAction: React.FC<DeleteActionProps> = (props: DeleteActionProps) => {
  const [deleting, setDeleting] = useState(false);
  const alerts = useAlerts();
  const mounted = useRef<boolean>(null);

  useEffect(() => {
    mounted.current = true;

    return () => {
      mounted.current = false;
    };
  });

  const ConfirmModal = ({isOpen, onResolve}: InstanceProps<boolean>) => (
    <DangerousConfirmModal confirmPhrase="delete upload" onAction={onResolve} show={isOpen}>
      <div className="mb-3">
        <p className="text-red-400 text-center text-lg mb-2">This action <span className="font-bold">cannot</span> be undone.</p>
      </div>
    </DangerousConfirmModal>
  );

  const ReasonModal = ({isOpen, onResolve}: InstanceProps<string>) => (
    <InputModal onComplete={onResolve} show={isOpen}>
      <p>Please specify the reason for moderator deletion.</p>
    </InputModal>
  );

  const CompleteModal = ({isOpen}: InstanceProps<string>) => (
    <Modal show={isOpen}>
      <Modal.Header>Deleted</Modal.Header>
      <Modal.Body>
        <p>The upload was deleted successfully. Click <a href="/" className="text-blue-400 hover:text-blue-500 underline">here</a> to go home.</p>
      </Modal.Body>
    </Modal>
  );

  function doDelete(reason: Nullable<string>): Promise<void> {
    return new Promise((resolve, reject) => {

      props.onDeleteInitiated();
      setDeleting(true);
      const opts: Options = reason != null ? (
        {
          body: Util.formatUrlEncodedBody({
            reason,
          }),
        }
      ) : {};

      KY.delete(`/api/upload/${props.upload.upload.id}`, opts).json<RouterResponse>().then(data => {
        const consumed = RouterResponseConsumer(data);
        if (consumed.success) {
          resolve();
        } else {
          alerts.add({
            type: AlertType.ERROR,
            header: 'Deletion Failed',
            body: (
              <>
                <p>Failed to delete the upload.</p>
                <p>{consumed.message}</p>
              </>
            ),
          });
          reject(new Error('Failed to delete the upload. API Response: ' + consumed.message));
        }
      }).catch(err => {
        alerts.add({
          type: AlertType.ERROR,
          header: 'Deletion Failed',
          body: (
            <>
              <p>Failed to delete the upload.</p>
              <p>{err?.toString() ?? 'Failed to delete. Check your connection and try again later.'}</p>
            </>
          ),
        });
        reject(err);
      }).then(() => {
        setDeleting(false);
      });
    });
  }

  function handleClick() {
    create(ConfirmModal)().then(confirmed => {
      if (confirmed === true) {
        if (props.addReason) {
          create(ReasonModal)().then(reason => {
            if (reason) {
              doDelete(reason).then(() => {
                create(CompleteModal)().then(() => {
                  // noop
                });
              }).catch(console.error);
            }
          }).catch(err => {
            console.error('Failed to handle deletion.', err);
            alerts.add({
              type: AlertType.ERROR,
              header: 'Deletion Failed',
              body: (
                <>
                  <p>Failed to delete the upload.</p>
                  <p>{err?.toString() ?? 'Failed to delete. Check your connection and try again later.'}</p>
                </>
              ),
            });
          });
        } else {
          doDelete(null).then(() => {
            create(CompleteModal)().then(() => {
              // noop
            }).catch(console.error);
          });
        }
      }
    }).catch(err => {
      console.error('Failed to handle deletion.', err);
      alerts.add({
        type: AlertType.ERROR,
        header: 'Deletion Failed',
        body: (
          <>
            <p>Failed to delete the upload.</p>
            <p>{err?.toString() ?? 'Failed to delete. Check your connection and try again later.'}</p>
          </>
        ),
      });
    });
  }

  return (
    <>
      <Container/>
      <Button variant="red" onClick={handleClick} disabled={deleting}>
        {deleting ? (
          <><Spinner inline/> Deleting...</>
        ) : (
          <><Icon path={mdiDeleteForever} size={1} className="mr-1 relative bottom-px inline-block"/>Delete</>
        )}
      </Button>
    </>
  );
};

export default DeleteAction;
