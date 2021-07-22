import clsx from 'clsx';
import * as React from 'react';

export type CenteredBlockPageProps = {
  children: React.ReactElement | React.ReactElement[] | string;
  cardBackground?: string;
  cardBorder?: string;
  pageBackground?: string;
  className?: string;
};

export default function CenteredBlockPage(props: CenteredBlockPageProps) {
  return (
    <div className={clsx('flex items-center justify-center h-full w-full', props.pageBackground)}>
      <div className={clsx(props.cardBackground || 'bg-gray-100', 'border', props.cardBorder || 'border-gray-200', 'p-5 rounded shadow w-11/12 sm:w-auto sm:min-w-96', props.className)}>
        {props.children}
      </div>
    </div>
  );
}
