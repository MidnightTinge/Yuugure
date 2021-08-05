import * as React from 'react';
import {FormEvent, useRef, useState} from 'react';
import KY from '../../classes/KY';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Button from '../../Components/Button';

import CenteredBlockPage from '../../Components/CenteredBlockPage';
import FormBlock from '../../Components/FormBlock';
import Spinner from '../../Components/Spinner';

type ValidationState = {
  invalid: boolean;
  error: string;
}

export type PageRegisterProps = {
  //
};

export const PageRegister: React.FC<PageRegisterProps> = () => {

  const txtEmail = useRef<HTMLInputElement>(null);
  const txtUsername = useRef<HTMLInputElement>(null);
  const txtPassword = useRef<HTMLInputElement>(null);
  const txtRepeat = useRef<HTMLInputElement>(null);

  const [emailInvalid, setEmailInvalid] = useState<ValidationState>({invalid: false, error: null});
  const [usernameInvalid, setUsernameInvalid] = useState<ValidationState>({invalid: false, error: null});
  const [passwordInvalid, setPasswordInvalid] = useState<ValidationState>({invalid: false, error: null});
  const [repeatInvalid, setRepeatInvalid] = useState<ValidationState>({invalid: false, error: null});

  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string>(null);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();

    setPosting(true);
    KY.post('/auth/register', {
      body: new URLSearchParams({
        email: txtEmail.current.value,
        username: txtUsername.current.value,
        password: txtPassword.current.value,
        repeat: txtRepeat.current.value,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<AuthResponse>(data);
      if (consumed.message) {
        setError(consumed.message);
      }

      const [authRes] = consumed.data;
      if (authRes.authed) {
        setEmailInvalid({invalid: false, error: null});
        setUsernameInvalid({invalid: false, error: null});
        setPasswordInvalid({invalid: false, error: null});
        setRepeatInvalid({invalid: false, error: null});
        setError(null);

        (document.location as any) = '/';
      } else {
        if (authRes.inputErrors.email) {
          setEmailInvalid({
            invalid: true,
            error: authRes.inputErrors.email.join('\n'),
          });
        } else {
          setEmailInvalid({
            invalid: false,
            error: null,
          });
        }

        if (authRes.inputErrors.username) {
          setUsernameInvalid({
            invalid: true,
            error: authRes.inputErrors.username.join('\n'),
          });
        } else {
          setUsernameInvalid({
            invalid: false,
            error: null,
          });
        }

        if (authRes.inputErrors.password) {
          setPasswordInvalid({
            invalid: true,
            error: authRes.inputErrors.password.join('\n'),
          });
        } else {
          setPasswordInvalid({
            invalid: false,
            error: null,
          });
        }

        if (authRes.inputErrors.repeat) {
          setRepeatInvalid({
            invalid: true,
            error: authRes.inputErrors.repeat.join('\n'),
          });
        } else {
          setRepeatInvalid({
            invalid: false,
            error: null,
          });
        }

        if (authRes.errors.length > 0) {
          setError(authRes.errors.join('\n'));
        }
      }
    }).catch(err => {
      setError(err.toString());
    }).then(() => {
      setPosting(false);
    });
  }

  return (
    <CenteredBlockPage>
      <p className="text-gray-600 text-xl text-center">Create Account</p>
      <form onSubmit={handleSubmit} method="post" action="/auth/login">
        <FormBlock ref={txtEmail} type="email" name="email" autoComplete="email" className="mb-2" invalid={emailInvalid.invalid} validationError={emailInvalid.error} disabled={posting} required>Email</FormBlock>
        <FormBlock ref={txtUsername} type="text" name="username" autoComplete="username" className="mb-2" invalid={usernameInvalid.invalid} validationError={usernameInvalid.error} disabled={posting} required>Username</FormBlock>
        <FormBlock ref={txtPassword} type="password" name="password" autoComplete="new-password" className="mb-2" invalid={passwordInvalid.invalid} validationError={passwordInvalid.error} disabled={posting} required>Password</FormBlock>
        <FormBlock ref={txtRepeat} type="password" name="repeat" autoComplete="new-password" className="mb-4" invalid={repeatInvalid.invalid} validationError={repeatInvalid.error} disabled={posting} required>Repeat</FormBlock>

        <Button type="submit" variant="green" disabled={posting} block>
          {posting ? (<><Spinner inline/> Creating account...</>) : 'Register'}
        </Button>
        {error != null ? (
          <p className="text-red-500 text-sm text-center font-mono my-2">{error}</p>
        ) : null}
        <div className="text-right">
          <a href="/auth/recover" className="text-sm text-blue-400 hover:text-blue-500 focus:text-blue-500 mr-3">Recover Account</a>
          <a href="/auth/login" className="text-sm text-blue-400 hover:text-blue-500 focus:text-blue-500">Login</a>
        </div>
      </form>
    </CenteredBlockPage>
  );
};

export default PageRegister;
