import {Switch} from '@headlessui/react';
import clsx from 'clsx';
import * as React from 'react';
import Spinner from '../Spinner';

export type ToggleSwitchProps = {
  checked: boolean;
  children: React.ReactNode;
  loading?: boolean;
  onChange?: (checked: boolean) => void;
};

export default function ToggleSwitch(props: ToggleSwitchProps) {
  function handleChange(checked: boolean) {
    if (typeof props.onChange === 'function') {
      props.onChange(checked);
    }
  }

  return (
    props.loading ? (
      <div className="inline-flex items-center cursor-wait">
        <span className="mr-2 cursor-pointer select-none">{props.children}</span>
        <div className="bg-gray-300 relative inline-flex items-center h-6 rounded-full w-11 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-green-200">
          <span className="translate-x-2 bg-gray-50 border-gray-200 border inline-block w-7 h-4 transform rounded-full transition-transform text-xs flex flex-row items-center justify-center text-gray-600">
            <Spinner/>
          </span>
        </div>
      </div>
    ) : (
      <Switch.Group>
        <div className="inline-flex items-center">
          <Switch.Label className="mr-2 cursor-pointer select-none">{props.children}</Switch.Label>
          <Switch checked={props.checked} onChange={handleChange} className={clsx(props.checked ? 'bg-green-300' : 'bg-gray-300', 'relative inline-flex items-center h-6 rounded-full w-11 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 focus:ring-green-200')}>
            <span className={clsx(props.checked ? 'translate-x-6 bg-white' : 'translate-x-1 bg-white', 'inline-block w-4 h-4 transform rounded-full transition-transform')}/>
          </Switch>
        </div>
      </Switch.Group>
    )
  );
}
