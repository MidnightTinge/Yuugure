import * as React from 'react';
import {useEffect, useState} from 'react';
import {create} from 'react-modal-promise';
import {InstanceProps} from 'react-modal-promise/lib/types';
import KY from '../../../classes/KY';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import Util from '../../../classes/Util';

import Modal from '../../../Components/Modal/Modal';
import ConfirmPasswordModal from '../../../Components/modals/ConfirmPasswordModal';
import DangerousConfirmModal from '../../../Components/modals/DangerousConfirmModal';
import ActionLoading from './ActionLoading';

type DangerousPasswordConfirmationOpts = {
  warningText: string;
  confirmPhrase: string;
}

export function makeDangerousPasswordConfirmation(opts: DangerousPasswordConfirmationOpts): Promise<{ authed: boolean, confirmToken: string }> {
  const ConfirmModal = ({isOpen, onResolve}: InstanceProps<boolean>) => (
    <DangerousConfirmModal show={isOpen} confirmPhrase={opts.confirmPhrase} onAction={onResolve}>
      <div className="mb-3">
        <p className="text-red-400 text-center text-lg mb-2">This action <span className="font-bold">cannot</span> be undone.</p>
        <p className="text-gray-600 text-sm">{opts.warningText}</p>
      </div>
    </DangerousConfirmModal>
  );
  const PasswordModal = ({isOpen, onResolve}: InstanceProps<Partial<AuthConfirmResponse>>) => (
    <ConfirmPasswordModal onComplete={(authenticated, confirmationToken) => onResolve({authenticated, confirmation_token: confirmationToken})} show={isOpen}/>
  );
  return new Promise((resolve, reject) => {
    create(ConfirmModal)().then(confirmed => {
      if (confirmed === true) {
        create(PasswordModal)().then(({authenticated, confirmation_token}) => {
          resolve({authed: authenticated, confirmToken: confirmation_token});
        }).catch(err => reject(err));
      } else {
        resolve({authed: false, confirmToken: null});
      }
    }).catch(err => reject(err));
  });
}

type DeleteStatusProps = {
  endpoint: string;
  resourceDescriptor: string;
  confirmationToken: string;
  onResolve: (...args: any[]) => any;
  show: boolean;
}

/**
 * WARNING: This is a DESTRUCTIVE element. It initiates deletion on MOUNT. Its use is utility only.
 *
 * @param props
 * @constructor
 */
export const ResourceDeletion: React.FC<DeleteStatusProps> = (props: DeleteStatusProps) => {
  const [posting, setPosting] = useState(false);
  const [posted, setPosted] = useState(false);
  const [res, setRes] = useState<string>(null);
  const [error, setError] = useState<string>(null);

  useEffect(function mounted() {
    setPosting(true);
    KY.delete(props.endpoint, {
      body: Util.formatUrlEncodedBody({
        confirmation_token: props.confirmationToken,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer(data);
      if (consumed.success) {
        setError(null);
        setRes(consumed.message);
      } else {
        setError(consumed.message);
      }
    }).catch(err => {
      setError(err.toString());
    }).then(() => {
      setPosting(false);
      setPosted(true);
    });
  }, []);

  function handleCloseRequest() {
    if (posted && typeof props.onResolve === 'function') {
      props.onResolve();
    }
  }

  return (
    <Modal show={props.show} onCloseRequest={handleCloseRequest}>
      <Modal.Header>{`${capitalize(props.resourceDescriptor)} Deletion`}</Modal.Header>
      <Modal.Body>
        <div className="text-center px-3 pb-3">
          <ActionLoading posting={posting} error={error} response={res}>{`Deleting ${capitalize(props.resourceDescriptor)}...`}</ActionLoading>
        </div>
      </Modal.Body>
    </Modal>
  );
};

export default ResourceDeletion;

function capitalize(str: string) {
  if (!str) return '';

  return str.charAt(0).toUpperCase() + str.substring(1);
}
