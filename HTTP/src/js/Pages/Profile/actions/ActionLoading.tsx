import {mdiCheckCircle} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import Spinner from '../../../Components/Spinner';

export type ActionLoadingProps = {
  posting: boolean;
  error: string;
  response: string;
  children?: React.ReactFragment;
};

export default function ActionLoading({posting, error, response, children}: ActionLoadingProps) {
  return (
    posting ? (
      <>
        <p className="text-xl text-gray-600 mb-2">{children || 'Working...'}</p>
        <Spinner size={4} className="text-gray-400"/>
      </>
    ) : (
      error ? (<p className="text-red-500 whitespace-pre-wrap">{error}</p>) : (
        <>
          <p className="text-green-700">{response || 'Job Done'}</p>
          <Icon path={mdiCheckCircle} size={4} className="text-green-500 mx-auto my-0"/>
        </>
      )
    )
  );
}
