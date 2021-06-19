import * as React from 'react';

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
        <i className="fas fa-spinner fa-pulse text-gray-400 fa-4x"/>
      </>
    ) : (
      error ? (<p className="text-red-500 whitespace-pre-wrap">{error}</p>) : (
        <>
          <p className="text-green-700">{response || 'Job Done'}</p>
          <i className="fas fa-check-circle text-green-500 fa-4x"/>
        </>
      )
    )
  );
}
