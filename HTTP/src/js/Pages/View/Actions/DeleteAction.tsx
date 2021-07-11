import * as React from 'react';
import {useEffect, useRef, useState} from 'react';
import {Container, create} from 'react-modal-promise';
import {InstanceProps} from 'react-modal-promise/lib/types';
import {useHistory} from 'react-router-dom';
import KY from '../../../classes/KY';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import {AlertType} from '../../../Components/Alerts/Alert/Alert';
import {useAlerts} from '../../../Components/Alerts/AlertsProvider';
import DangerousConfirmModal from '../../../Components/modals/DangerousConfirmModal';
import Spinner from '../../../Components/Spinner';

export type DeleteActionProps = {
  upload: RenderableUpload;
  onDeleteInitiated: () => void;
};

export default function DeleteAction(props: DeleteActionProps) {
  const [deleting, setDeleting] = useState(false);
  const [mount, setMount] = useState(true);
  const history = useHistory();
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

  function doDelete() {
    props.onDeleteInitiated();
    setDeleting(true);
    KY.delete(`/api/upload/${props.upload.upload.id}`).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer(data);
      if (consumed.success) {
        alerts.add({
          type: AlertType.SUCCESS,
          header: 'Upload Deleted',
          body: 'The upload was deleted successfully.',
        });
        setTimeout(() => {
          history.push('/');
        }, 250);
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
    }).then(() => {
      if (mounted.current) {
        setDeleting(false);
      }
    });
  }

  function handleClick() {
    create(ConfirmModal)().then(confirmed => {
      // react-modal-promise sets a timeout to do some state cleanup on dismount. this leads to
      // harmless errors in console if we redirect before dismounting but best to let it do its
      // thing regardless.
      setMount(false);
      if (confirmed === true) {
        doDelete();
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
      {mount ? (<Container/>) : null}
      <button className="inline-block px-4 py-1 font-semibold rounded text-white border border-red-500 bg-red-400 hover:bg-red-500 disabled:bg-red-600 disabled:hover:bg-red-600 disabled:cursor-not-allowed" onClick={handleClick} disabled={deleting}>
        {deleting ? (
          <><Spinner/> Deleting...</>
        ) : (
          <><i className="fas fa-times-circle mr-1" aria-hidden={true}/>Delete</>
        )}
      </button>
    </>
  );
}
