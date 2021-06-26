import * as React from 'react';
import {useRef, useState} from 'react';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import {XHR} from '../../../classes/XHR';
import FormBlock from '../../../Components/FormBlock';
import Modal from '../../../Components/Modal/Modal';
import Spinner from '../../../Components/Spinner';

export type UpdatePasswordProps = {
  //
};

export default function UpdatePassword(props: UpdatePasswordProps) {
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
    XHR.for('/api/account/@me/password').patch(XHR.BODY_TYPE.FORM, {
      newPassword: txtNewPassword.current.value,
      repeat: txtRepeat.current.value,
      currentPassword: txtCurrentPassword.current.value,
    }).getJson<RouterResponse<InputAwareResponse<any>>>().then(resp => {
      let consumed = RouterResponseConsumer(resp, 'AccountUpdateResponse');
      if (consumed.success) {
        setPosted(true);
      } else {
        setError(consumed.message);

        let [authRes] = consumed.data;
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
                <button type="button" className="px-2 bg-blue-300 border border-blue-400 hover:bg-blue-400 hover:border-blue-500 rounded-md cursor-pointer mr-2" onClick={handleCloseRequest}>Cancel</button>
                <button type="submit" className="px-2 bg-red-300 border border-red-400 hover:bg-red-400 hover:border-red-500 rounded-md cursor-pointer">{
                  posting ? (<><Spinner/> Working...</>) : ('Confirm')
                }</button>
              </div>
            </form>
          ) : (
            !error ? (
              <div className="text-center px-3 pb-3">
                <p className="text-lg">Password Updated</p>
                <i className="fas fa-check-circle fa-4x text-green-500"/>
              </div>
            ) : null
          )}
          {error ? (
            <p className="my-2 text-red-500 whitespace-pre-wrap">{error}</p>
          ) : null}
        </Modal.Body>
      </Modal>
      <div>
        <button className="text-blue-400 hover:text-blue-500 focus:outline-none underline" onClick={handleAction}>Update Password</button>
      </div>
    </>
  );
}
