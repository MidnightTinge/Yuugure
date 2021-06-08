import * as React from 'react';
import {Provider} from 'react-redux';
import {authStateStore, authStateSelector, reloadAuthState} from '../../Stores/AuthStore';
import Nav, {NavActive} from '../Nav/Nav';

export type PageRendererProps = {
  children: React.ReactElement;
  active: NavActive;
};

export default function PageRenderer(props: PageRendererProps) {
  const authState = authStateSelector();
  if (!authState.fetched && !authState.fetching) {
    reloadAuthState();
  }

  return (
    <div className="flex flex-col w-full h-full">
      <Nav active={props.active}/>
      {props.children}
    </div>
  );
}
