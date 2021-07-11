import {mdiBookmark, mdiCheckCircle} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import {useRef, useState} from 'react';
import KY from '../../../classes/KY';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import Util from '../../../classes/Util';
import Button from '../../../Components/Button';

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
    KY.patch('/api/account/@me/email', {
      body: Util.formatUrlEncodedBody({
        email: txtEmail.current.value,
        password: txtPassword.current.value,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<InputAwareResponse<any>>(data);
      if (consumed.success) {
        setPosted(true);
      } else {
        setError(consumed.message);

        // Set any input errors as necessary
        let [authRes] = consumed.data;
        if (authRes) {
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
                <Button type="button" variant="blue" onClick={handleCloseRequest}>Cancel</Button>
                <Button type="submit" variant="red">{
                  posting ? (<><Spinner/> Working...</>) : ('Confirm')
                }</Button>
              </div>
            </form>
          ) : (
            !error ? (
              <div className="text-center px-3 pb-3">
                <p className="text-lg">Email Updated</p>
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
        <Button variant="blue" onClick={handleAction} link>Update Email</Button>
      </div>
    </>
  );
}
