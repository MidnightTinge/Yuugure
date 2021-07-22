import * as React from 'react';
import {FormEvent, useRef, useState} from 'react';
import KY from '../../classes/KY';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Util from '../../classes/Util';
import Button from '../../Components/Button';

import CenteredBlockPage from '../../Components/CenteredBlockPage';
import FormBlock from '../../Components/FormBlock';
import Spinner from '../../Components/Spinner';

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
    KY.post('/auth/login', {
      body: Util.formatUrlEncodedBody({
        email: txtEmail.current.value,
        password: txtPassword.current.value,
      }),
      throwHttpErrors: false,
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<AuthResponse>(data);
      if (consumed.success) {
        let [authRes] = consumed.data;
        if (authRes.authed) {
          setPwInvalid(false);
          setEmailInvalid(false);
          setError(null);

          (document.location as any) = '/';
        }
      }

      if (consumed.message) {
        setError(consumed.message);
      }

      let [authRes] = consumed.data;
      if ('email' in authRes.inputErrors) {
        setEmailInvalid(true);
        setEmailError(authRes.inputErrors.email.join('\n'));
      } else {
        setEmailInvalid(false);
        setEmailError(null);
      }

      if ('password' in authRes.inputErrors) {
        setPwInvalid(true);
        setPwError(authRes.inputErrors.password.join('\n'));
      } else {
        setPwInvalid(false);
        setPwError(null);
      }

      if (authRes.errors.length > 0) {
        setError(authRes.errors.join('\n'));
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

        <Button type="submit" variant="green" disabled={posting} block>
          {posting ? (<><Spinner inline/> Logging in...</>) : `Log In`}
        </Button>
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
