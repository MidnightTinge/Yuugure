import * as React from 'react';
import {useRef, useState} from 'react';
import KY from '../../classes/KY';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Button from '../Button';
import FormBlock from '../FormBlock';
import Modal from '../Modal/Modal';
import Spinner from '../Spinner';

export type ConfirmPasswordModalProps = {
  onComplete: (authenticated: boolean, confirmationToken: string) => void;
  show: boolean;
};

export const ConfirmPasswordModal: React.FC<ConfirmPasswordModalProps> = (props: ConfirmPasswordModalProps) => {
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string>(null);
  const [pwValidity, setPwValidity] = useState<InputValidity>({valid: true, error: null});

  const txtPassword = useRef<HTMLInputElement>(null);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setPosting(true);

    KY.post('/auth/confirm', {
      body: new URLSearchParams({
        password: txtPassword.current.value,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<AuthConfirmResponse>(data);
      if (consumed.success) {
        const [authRes] = consumed.data;
        if (!authRes.authenticated) {
          if ('password' in authRes.inputErrors) {
            setPwValidity({valid: false, error: authRes.inputErrors.password.join('\n')});
          } else {
            setPwValidity({valid: true, error: null});
          }
        } else {
          props.onComplete(true, authRes.confirmation_token);
        }
      } else {
        setError(consumed.message);
      }
    }).catch(err => {
      setError(err.toString());
    }).then(() => {
      setPosting(false);
    });
  }

  function handleCloseRequest() {
    if (!posting) {
      props.onComplete(false, null);
    }
  }

  function handleCancelClick() {
    props.onComplete(false, null);
  }

  return (
    <Modal show={props.show} onCloseRequest={handleCloseRequest} closeButton={!posting}>
      <Modal.Header>Confirm Password</Modal.Header>
      <Modal.Body>
        <p>Please confirm your password.</p>
        <form method="POST" action="#" onSubmit={handleSubmit}>
          <FormBlock ref={txtPassword} type="password" name="password" autoComplete="current-password" disabled={posting} invalid={!pwValidity.valid} validationError={pwValidity.error} required>
            Current Password
          </FormBlock>
          {error ? (
            <p className="my-2 text-red-500 whitespace-pre-wrap">{error}</p>
          ) : null}
          <div className="text-right mt-2">
            <Button variant="gray" type="button" disabled={posting} onClick={handleCancelClick}>Cancel</Button>
            <Button variant="blue" type="submit" disabled={posting}>
              {!posting ? 'Submit' : (
                <><Spinner inline/> Working...</>
              )}
            </Button>
          </div>
        </form>
      </Modal.Body>
    </Modal>
  );
};

export default ConfirmPasswordModal;
