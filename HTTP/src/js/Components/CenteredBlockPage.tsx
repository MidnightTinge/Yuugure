import * as React from 'react';

export type CenteredBlockPageProps = {
  children: React.ReactElement | React.ReactElement[] | string;
  cardBackground?: string;
  pageBackground?: string;
  className?: string;
};

export default function CenteredBlockPage(props: CenteredBlockPageProps) {
  return (
    <div className={`flex items-center justify-center h-screen w-screen ${props.pageBackground || 'bg-gray-50'}`}>
      <div className={`${props.cardBackground || 'bg-gray-200'} border border-gray-300 px-5 pt-5 pb-2 rounded shadow w-11/12 sm:w-96 ${props.className || ''}`}>
        {props.children}
      </div>
    </div>
  );
}
