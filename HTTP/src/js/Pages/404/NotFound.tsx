import * as React from 'react';
import {useHistory} from 'react-router-dom';
import CenteredBlockPage from '../../Components/CenteredBlockPage';

export type NotFoundProps = {
  //
};

export const NotFound: React.FC<NotFoundProps> = () => {
  const history = useHistory();

  function handleHomeNavigation(e: React.MouseEvent<HTMLAnchorElement, MouseEvent>) {
    e.preventDefault();
    history.push('/');
  }

  return (
    <CenteredBlockPage pageBackground="bg-yellow-50" cardBackground="bg-yellow-200">
      <p className="text-lg text-center mb-6">Not Found</p>
      <p>The resource you requested could not be found. Click <a href="/" className="underline text-blue-500 hover:text-blue-600" onClick={handleHomeNavigation}>here</a> to go home.</p>
    </CenteredBlockPage>
  );
};

export default NotFound;
