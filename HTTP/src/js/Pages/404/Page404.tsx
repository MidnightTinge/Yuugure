import * as React from 'react';
import CenteredBlockPage from '../../Components/CenteredBlockPage';

export type Page404Props = {
  //
};

export default function Page404(props: Page404Props) {
  return (
    <CenteredBlockPage pageBackground="bg-yellow-50" cardBackground="bg-yellow-200">
      <p className="text-lg text-center mb-6">Not Found</p>
      <p>The resource you requested could not be found. Click <a href="/" className="underline text-blue-500 hover:text-blue-600">here</a> to go home.</p>
    </CenteredBlockPage>
  );
}
