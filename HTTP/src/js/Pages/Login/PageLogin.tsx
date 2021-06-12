import {FormEvent, useEffect, useRef, useState} from 'react';
import * as React from 'react';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faSpinnerThird} from '@fortawesome/pro-solid-svg-icons';
import {XHR} from '../../classes/XHR';
import FormBlock from '../../Components/FormBlock';
import CenteredBlockPage from '../../Components/CenteredBlockPage';

export type LoginProps = {
  //
};

export default function PageLogin(props: LoginProps) {
  const txtEmail = useRef<HTMLInputElement>(null);
  const txtPassword = useRef<HTMLInputElement>(null);

  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string>(null);

  const [emailInvalid, setEmailInvalid] = useState(false);
  const [emailError, setEmailError] = useState<string>(null);
  const [pwInvalid, setPwInvalid] = useState(false);
  const [pwError, setPwError] = useState<string>(null);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();

    setPosting(true);
    XHR.for('/auth/login').post(XHR.BODY_TYPE.FORM, {
      email: txtEmail.current.value,
      password: txtPassword.current.value,
    }).getJson<RouterResponse<AuthResponse>>().then(data => {
      if (data && data.data) {
        const authRes = data.data.AuthResponse[0] as AuthResponse;
        if (authRes) {
          if (authRes.authed) {
            setPwInvalid(false);
            setEmailInvalid(false);
            setError(null);

            (document.location as any) = '/';
          } else {
            if (authRes.inputErrors.email) {
              setEmailInvalid(true);
              setEmailError(authRes.inputErrors.email.join('\n'));
            } else {
              setEmailInvalid(false);
              setEmailError(null);
            }

            if (authRes.inputErrors.password) {
              setPwInvalid(true);
              setPwError(authRes.inputErrors.password.join('\n'));
            } else {
              setPwInvalid(false);
              setPwError(null);
            }

            if (authRes.errors.length > 0) {
              setError(authRes.errors.join('\n'));
            }
          }
        } else {
          setError('The server returned an invalid response. Please try again.');
        }
      } else {
        setError('An unknown error occurred. Please try again later.');
      }
    }).catch(err => {
      setError(err.toString());
    }).then(() => {
      setPosting(false);
    });
  }

  return (
    <CenteredBlockPage>
      <p className="text-gray-600 text-xl text-center">Log In</p>
      <form onSubmit={handleSubmit} method="post" action="/auth/login">
        <FormBlock ref={txtEmail} type="text" name="email" className="mb-2" autoComplete="email" invalid={emailInvalid} validationError={emailError} disabled={posting} required>Email</FormBlock>
        <FormBlock ref={txtPassword} type="password" name="password" className="mb-3" autoComplete="current-password" invalid={pwInvalid} validationError={pwError} disabled={posting} required>Password</FormBlock>

        <button type="submit" className="block w-full bg-green-500 py-1 shadow-sm rounded text-white hover:bg-green-600 focus:bg-green-600 disabled:cursor-not-allowed disabled:bg-green-700 disabled:text-gray-400" disabled={posting}>
          {posting ? (
            <>
              <FontAwesomeIcon icon={faSpinnerThird} spin/> Logging in...
            </>
          ) : `Log In`}
        </button>
        {error != null ? (
          <p className="text-red-500 text-sm text-center font-mono my-2">{error}</p>
        ) : null}
        <div className="text-right">
          <a href="/auth/recover" className="text-sm text-blue-400 hover:text-blue-500 focus:text-blue-500 mr-3">Recover Account</a>
          <a href="/auth/register" className="text-sm text-blue-400 hover:text-blue-500 focus:text-blue-500">Register</a>
        </div>
      </form>
    </CenteredBlockPage>
  );
}