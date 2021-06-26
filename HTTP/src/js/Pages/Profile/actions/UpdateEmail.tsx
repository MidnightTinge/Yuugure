import * as React from 'react';
import {useRef, useState} from 'react';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import {XHR} from '../../../classes/XHR';
import FormBlock from '../../../Components/FormBlock';
import Modal from '../../../Components/Modal/Modal';
import Spinner from '../../../Components/Spinner';

export type UpdateEmailProps = {
  //
};

export default function UpdateEmail(props: UpdateEmailProps) {
  const [showModal, setShowModal] = useState(false);
  const [posting, setPosting] = useState(false);
  const [posted, setPosted] = useState(false);
  const [error, setError] = useState<string>(null);

  const [emailValidity, setEmailValidity] = useState<InputValidity>({valid: true});
  const [passwordValidity, setPasswordValidity] = useState<InputValidity>({valid: true});

  const txtEmail = useRef<HTMLInputElement>(null);
  const txtPassword = useRef<HTMLInputElement>(null);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setPosting(true);
    setPosted(false);
    XHR.for('/api/account/@me/email').patch(XHR.BODY_TYPE.FORM, {
      email: txtEmail.current.value,
      password: txtPassword.current.value,
    }).getJson<RouterResponse<InputAwareResponse<any>>>().then(resp => {
      let consumed = RouterResponseConsumer(resp, 'AccountUpdateResponse');
      if (consumed.success) {
        setPosted(true);
      } else {
        setError(consumed.message);

        // Set any input errors as necessary
        let [authRes] = consumed.data;
        if (authRes) {
          let authRes: InputAwareResponse<any> = resp.data.AccountUpdateResponse[0];
          if ('email' in authRes.inputErrors) {
            setEmailValidity({valid: false, error: authRes.inputErrors.email.join('\n')});
          } else {
            setEmailValidity({valid: true});
          }
          if ('password' in authRes.inputErrors) {
            setPasswordValidity({valid: false, error: authRes.inputErrors.password.join('\n')});
          } else {
            setPasswordValidity({valid: true});
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
    setEmailValidity({valid: true});
    setPasswordValidity({valid: true});
  }

  return (
    <>
      <Modal show={showModal} onCloseRequest={handleCloseRequest} onClose={handleModalClosed} closeButton>
        <Modal.Header>Update Email</Modal.Header>
        <Modal.Body>
          {!posted ? (
            <form method="POST" action="#" onSubmit={handleSubmit}>
              <FormBlock ref={txtEmail} type="email" name="email" autoComplete="email" invalid={!emailValidity.valid} validationError={emailValidity.error} required>
                New Email
              </FormBlock>
              <FormBlock ref={txtPassword} type="password" name="password" autoComplete="current-password" className="mt-2 mb-3" invalid={!passwordValidity.valid} validationError={passwordValidity.error} required>
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
                <p className="text-lg">Email Updated</p>
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
        <button className="text-blue-400 hover:text-blue-500 focus:outline-none underline" onClick={handleAction}>Update Email</button>
      </div>
    </>
  );
}
