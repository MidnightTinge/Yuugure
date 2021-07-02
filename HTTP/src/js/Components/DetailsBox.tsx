import * as React from 'react';

export type DetailsBoxProps = {
  header: string;
  children: React.ReactFragment;
};

export default function DetailsBox({header, children}: DetailsBoxProps) {
  return (
    <div className="rounded bg-gray-200 border border-gray-300 shadow-sm">
      <div className="py-0.5 text-center text-gray-800 border-b border-gray-300">{header}</div>
      <div className="p-2">
        {children}
      </div>
    </div>
  );
}
