import clsx from 'clsx';
import * as React from 'react';
import {ForwardedRef} from 'react';

export type FormBlockProps = {
  type: string;
  name: string;
  children?: React.ReactElement | React.ReactElement[] | string;
  disabled?: boolean;
  autoComplete?: string;
  required?: boolean;
  className?: string;

  invalid?: boolean;
  validationError?: string;
};

const FormBlock = React.forwardRef((props: FormBlockProps, ref: ForwardedRef<HTMLInputElement>) => (
  <label className={clsx('block', props.className)}>
    <span className="text-gray-500">{props.children}</span>
    <input ref={ref} type={props.type} name={props.name} autoComplete={props.autoComplete} disabled={props.disabled === true} required={props.required === true} className={clsx('block w-full rounded-md', props.invalid ? 'border-red-500 hover:border-red-300' : 'border-gray-300 hover:bg-gray-50', 'shadow focus:border-gray-500 focus:ring focus:ring-gray-400 focus:ring-opacity-50 disabled:cursor-not-allowed disabled:bg-gray-200')}/>
    {props.invalid && props.validationError ? (
      <small className="text-red-600 block ml-1">{props.validationError}</small>
    ) : null}
  </label>
));

export default FormBlock;
