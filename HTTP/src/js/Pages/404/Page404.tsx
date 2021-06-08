import * as React from 'react';

export type Page404Props = {
  //
};

export default function Page404(props: Page404Props) {
  return (
    <div className="flex items-center justify-center h-screen w-screen bg-yellow-50">
      <div className="bg-yellow-200 border border-yellow-300 p-5 rounded shadow w-11/12 sm:w-96">
        <p className="text-lg text-center mb-6">Not Found</p>
        <p>The resource you requested could not be found. Click <a href="/" className="underline text-blue-500 hover:text-blue-600">here</a> to go home.</p>
      </div>
    </div>
  );
}
