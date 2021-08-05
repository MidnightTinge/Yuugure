import {mdiCheckCircle} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import {useRef, useState} from 'react';
import KY from '../../../classes/KY';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import Button from '../../../Components/Button';

import FormBlock from '../../../Components/FormBlock';
import Modal from '../../../Components/Modal/Modal';
import Spinner from '../../../Components/Spinner';

export type UpdatePasswordProps = {
  //
};

export const UpdatePassword: React.FC<UpdatePasswordProps> = () => {
  const [showModal, setShowModal] = useState(false);
  const [posting, setPosting] = useState(false);
  const [posted, setPosted] = useState(false);
  const [error, setError] = useState<string>(null);

  const [passwordValidity, setPasswordValidity] = useState<InputValidity>({valid: true});
  const [repeatValidity, setRepeatValidity] = useState<InputValidity>({valid: true});
  const [currentValidity, setCurrentValidity] = useState<InputValidity>({valid: true});

  const txtNewPassword = useRef<HTMLInputElement>(null);
  const txtRepeat = useRef<HTMLInputElement>(null);
  const txtCurrentPassword = useRef<HTMLInputElement>(null);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setPosting(true);
    setPosted(false);
    KY.patch('/api/account/@me/password', {
      body: new URLSearchParams({
        newPassword: txtNewPassword.current.value,
        repeat: txtRepeat.current.value,
        currentPassword: txtCurrentPassword.current.value,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<InputAwareResponse<any>>(data);
      if (consumed.success) {
        setPosted(true);
      } else {
        setError(consumed.message);

        const [authRes] = consumed.data;
        if (authRes) {
          if ('current' in authRes.inputErrors) {
            setCurrentValidity({valid: false, error: authRes.inputErrors.current.join('\n')});
          } else {
            setCurrentValidity({valid: true});
          }
          if ('password' in authRes.inputErrors) {
            setPasswordValidity({valid: false, error: authRes.inputErrors.password.join('\n')});
          } else {
            setPasswordValidity({valid: true});
          }
          if ('repeat' in authRes.inputErrors) {
            setRepeatValidity({valid: false, error: authRes.inputErrors.repeat.join('\n')});
          } else {
            setRepeatValidity({valid: true});
          }
        }
      }
    }).catch(err => {
      setError(err.toString());
    }).then(() => {
      setPosting(false);
    });
  }

  function handleCloseRequest() {
    if (!posting) {
      setShowModal(false);
    }
  }

  function handleAction() {
    setShowModal(true);
  }

  function handleModalClosed() {
    setShowModal(false);
    setPosting(false);
    setPosted(false);
    setError(null);
    setPasswordValidity({valid: true});
    setRepeatValidity({valid: true});
    setCurrentValidity({valid: true});
  }

  return (
    <>
      <Modal show={showModal} onCloseRequest={handleCloseRequest} onClose={handleModalClosed} closeButton>
        <Modal.Header>Update Password</Modal.Header>
        <Modal.Body>
          {!posted ? (
            <form method="POST" action="#" onSubmit={handleSubmit}>
              <FormBlock ref={txtNewPassword} type="password" name="repeat" autoComplete="newPassword" className="my-2" invalid={!passwordValidity.valid} validationError={passwordValidity.error} required>
                New Password
              </FormBlock>
              <FormBlock ref={txtRepeat} type="password" name="repeat" autoComplete="repeat" className="my-2" invalid={!repeatValidity.valid} validationError={repeatValidity.error} required>
                Repeat Password
              </FormBlock>
              <FormBlock ref={txtCurrentPassword} type="password" name="password" autoComplete="currentPassword" className="mt-2 mb-3" invalid={!currentValidity.valid} validationError={currentValidity.error} required>
                Current Password
              </FormBlock>

              <div className="text-right mt-2">
                <Button type="button" variant="blue" onClick={handleCloseRequest}>Cancel</Button>
                <Button type="submit" variant="red">{
                  posting ? (<><Spinner inline/> Working...</>) : ('Confirm')
                }</Button>
              </div>
            </form>
          ) : (
            !error ? (
              <div className="text-center px-3 pb-3">
                <p className="text-lg">Password Updated</p>
                <Icon path={mdiCheckCircle} size={4} className="text-green-500 mx-auto my-0"/>
              </div>
            ) : null
          )}
          {error ? (
            <p className="my-2 text-red-500 whitespace-pre-wrap">{error}</p>
          ) : null}
        </Modal.Body>
      </Modal>
      <div>
        <Button variant="blue" onClick={handleAction} link>Update Password</Button>
      </div>
    </>
  );
};

export default UpdatePassword;
