import KY from '../classes/KY';
import * as React from 'react';
import {useContext, useState} from 'react';
import {Link} from 'react-router-dom';
import {AuthStateContext} from '../Context/AuthStateProvider';
import Spinner from './Spinner';

export type NavActive = 'index' | 'upload' | 'view' | 'profile' | 'search' | 'login' | 'register' | '404';

function NavList({children}: { children: React.ReactElement | React.ReactElement[] }) {
  return (
    <ul className="list-none m-0 p-0 flex flex-row flex-nowrap">
      {children}
    </ul>
  );
}

function NavItem({children, href, active, onClick}: { children: React.ReactElement | React.ReactElement[] | string, href: string, active?: boolean, onClick?: (...args: any[]) => any }) {
  return (
    <li className={`p-2 mr-1 ${active ? 'bg-gray-300' : ''}`}>
      <Link className="hover:text-gray-800 focus:text-gray-800" to={href} onClick={onClick}>{children}</Link>
    </li>
  );
}

export type NavProps = {
  active?: NavActive
};

export default function Nav(props: NavProps) {
  const [loggingOut, setLoggingOut] = useState(false);

  const {state: authState, reloadAuth} = useContext(AuthStateContext);

  function handleLogoutClick(e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) {
    e.preventDefault();
    setLoggingOut(true);
    KY.get('/auth/logout').json().then(() => {
      // nothing to do
    }).catch((err) => {
      console.error('[logout]', err);
    }).then(() => {
      setLoggingOut(false);
      reloadAuth();
    });
  }

  return (
    <nav className="w-full h-nav px-4 flex-grow-0 flex-shrink flex flex-row flex-nowrap bg-gray-200 text-gray-500 border-solid border-b border-gray-300">
      <div className="mr-auto">
        <NavList>
          <NavItem href="/" active={props.active === 'index'}>Home</NavItem>
          {authState.authed ? (
            <NavItem href="/upload" active={props.active === 'upload'}>Upload</NavItem>
          ) : null}
        </NavList>
      </div>
      <div>
        <NavList>
          <NavItem href="/search" active={props.active === 'search'}>Search</NavItem>
          {authState.authed ? (
            <>
              <NavItem href={`/user/${authState.account.id}?settings`}>Profile</NavItem>
              {loggingOut ? (
                <li className={`p-2 mr-1`}>
                  <span className="text-gray-300 cursor-not-allowed select-none"><Spinner inline/> Logging out...</span>
                </li>
              ) : (
                <NavItem href="/auth/logout" onClick={handleLogoutClick}>Logout</NavItem>
              )}
            </>
          ) : (
            <>
              <NavItem href="/auth/login" active={props.active === 'login'}>Login</NavItem>
              <NavItem href="/auth/register" active={props.active === 'register'}>Register</NavItem>
            </>
          )}
        </NavList>
      </div>
    </nav>
  );
}
