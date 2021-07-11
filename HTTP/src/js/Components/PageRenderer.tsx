import {mdiAlertOctagon} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import {useContext} from 'react';
import {AuthStateContext} from '../Context/AuthStateProvider';
import CenteredBlockPage from './CenteredBlockPage';
import Nav, {NavActive} from './Nav';

export type PageRendererProps = {
  children: React.ReactElement;
  active: NavActive;
  authControlled?: true;
};

export default function PageRenderer(props: PageRendererProps) {
  const {state: authState} = useContext(AuthStateContext);

  let toRender = props.children;
  if (props.authControlled && !authState.authed) {
    toRender = (
      <CenteredBlockPage pageBackground="bg-red-100" cardBackground="bg-red-400" className="text-white">
        <div className="text-center">
          <p className="text-2xl font-bold mb-1">Access Restricted</p>
          <Icon path={mdiAlertOctagon} size={3} style={{margin: '0 auto'}}/>
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
