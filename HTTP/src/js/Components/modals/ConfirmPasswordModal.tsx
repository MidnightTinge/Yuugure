import * as React from 'react';
import {useRef, useState} from 'react';
import {XHR} from '../../classes/XHR';
import FormBlock from '../FormBlock';
import Modal from '../Modal/Modal';
import ModalBody from '../Modal/ModalBody';
import ModalHeader from '../Modal/ModalHeader';

export type ConfirmPasswordModalProps = {
  onComplete: (authenticated: boolean, confirmationToken: string) => void;
  show: boolean;
};

export default function ConfirmPasswordModal(props: ConfirmPasswordModalProps) {
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string>(null);
  const [pwValidity, setPwValidity] = useState<InputValidity>({valid: true, error: null});

  const txtPassword = useRef<HTMLInputElement>(null);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setPosting(true);
    XHR.for('/auth/confirm').post(XHR.BODY_TYPE.FORM, {
      password: txtPassword.current.value,
    }).getJson<RouterResponse<AuthConfirmResponse>>().then(resp => {
      if (resp) {
        if (resp.code === 200) {
          if (Array.isArray(resp.data.AuthConfirmResponse) && resp.data.AuthConfirmResponse[0]) {
            let authRes: AuthConfirmResponse = resp.data.AuthConfirmResponse[0];
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
            console.error('invalid /auth/confirm response:', resp);
            setError('An internal server error occurred. Please try again later.');
          }
        } else {
          console.error('bad data from `/auth/confirm`:', resp);
          setError('Failed to submit password. Please reload and try again.');
        }
      } else {
        setError('An internal server error occurred. Please try again later.');
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
      <ModalHeader>Confirm Password</ModalHeader>
      <ModalBody>
        <p>Please confirm your password.</p>
        <form method="POST" action="#" onSubmit={handleSubmit}>
          <FormBlock ref={txtPassword} type="password" name="password" autoComplete="current-password" disabled={posting} invalid={!pwValidity.valid} validationError={pwValidity.error} required>
            Current Password
          </FormBlock>
          {error ? (
            <p className="my-2 text-red-500 whitespace-pre-wrap">{error}</p>
          ) : null}
          <div className="text-right mt-2">
            <button type="button" disabled={posting} className="px-2 bg-gray-200 border border-gray-300 hover:bg-gray-300 hover:border-gray-400 rounded-md cursor-pointer mr-2 disabled:bg-gray-100 disabled:border-gray-200 disabled:text-gray-300 disabled:cursor-not-allowed" onClick={handleCancelClick}>Cancel</button>
            <button type="submit" disabled={posting} className="px-2 bg-blue-300 border border-blue-400 hover:bg-blue-400 hover:border-blue-500 rounded-md cursor-pointer disabled:bg-blue-200 disabled:border-blue-300 disabled:text-blue-300 disabled:cursor-not-allowed">
              {!posting ? 'Submit' : (
                <><i className="fas fa-spinner fa-pulse"/> Working...</>
              )}
            </button>
          </div>
        </form>
      </ModalBody>
    </Modal>
  );
}
