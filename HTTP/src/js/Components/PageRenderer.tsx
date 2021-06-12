import * as React from 'react';
import {authStateSelector, reloadAuthState} from '../Stores/AuthStore';
import CenteredBlockPage from './CenteredBlockPage';
import Nav, {NavActive} from './Nav';

export type PageRendererProps = {
  children: React.ReactElement;
  active: NavActive;
  authControlled?: true;
};

export default function PageRenderer(props: PageRendererProps) {
  const authState = authStateSelector();
  if (!authState.fetched && !authState.fetching) {
    reloadAuthState();
  }

  let toRender = props.children;
  if (props.authControlled && !authState.authed) {
    toRender = (
      <CenteredBlockPage pageBackground="bg-red-100" cardBackground="bg-red-400" className="text-white">
        <div className="text-center">
          <p className="text-xl font-bold mb-4">Access Restricted</p>
          <i className="fas fa-exclamation-triangle fa-3x mb-4"/>
          <p>You must be logged in to view this resource.</p>
        </div>
      </CenteredBlockPage>
    );
  }

  return (
    <div className="flex flex-col w-full h-full">
      <Nav active={props.active}/>
      {toRender}
    </div>
  );
}
