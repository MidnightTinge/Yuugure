import clsx from 'clsx';
import * as React from 'react';

export type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant: 'gray' | 'red' | 'yellow' | 'green' | 'blue' | 'indigo' | 'purple' | 'pink';
  link?: boolean;
  block?: boolean;
};

const BUTTON_BASE = 'inline-block disabled:cursor-not-allowed';

const BASE_FOR_BLOCK = 'px-4 py-1 font-semibold rounded text-white border';
const BASE_FOR_LINK = 'underline';

// note: can't extract these out to functions because of purgecss. we need to statically define all
//       of these classes in a way that purgecss can detect.
const BLOCK_VARIANTS = Object.freeze({
  gray: 'border-gray-500 bg-gray-400 hover:bg-gray-500',
  red: 'border-red-500 bg-red-400 hover:bg-red-500 disabled:bg-red-600 disabled:hover:bg-red-600',
  yellow: 'border-yellow-400 bg-yellow-300 hover:bg-yellow-400 disabled:bg-yellow-500 disabled:hover:bg-yellow-500 text-gray-700',
  green: 'border-green-500 bg-green-400 hover:bg-green-500 disabled:bg-green-600 disabled:hover:bg-green-600',
  blue: 'border-blue-500 bg-blue-400 hover:bg-blue-500 disabled:bg-blue-600 disabled:hover:bg-blue-600',
  indigo: 'border-indigo-500 bg-indigo-400 hover:bg-indigo-500 disabled:bg-indigo-600 disabled:hover:bg-indigo-600',
  purple: 'border-purple-500 bg-purple-400 hover:bg-purple-500 disabled:bg-purple-600 disabled:hover:bg-purple-600',
  pink: 'border-pink-500 bg-pink-400 hover:bg-pink-500 disabled:bg-pink-600 disabled:hover:bg-pink-600',
});
const LINK_VARIANTS = Object.freeze({
  gray: 'text-gray-500 hover:text-gray-700 disabled:text-gray-800 disabled:hover:text-gray-800',
  red: 'text-red-500 hover:text-red-700 disabled:text-red-800 disabled:hover:text-red-800',
  yellow: 'text-yellow-500 hover:text-yellow-700 disabled:text-yellow-800 disabled:hover:text-yellow-800',
  green: 'text-green-500 hover:text-green-700 disabled:text-green-800 disabled:hover:text-green-800',
  blue: 'text-blue-500 hover:text-blue-700 disabled:text-blue-800 disabled:hover:text-blue-800',
  indigo: 'text-indigo-500 hover:text-indigo-700 disabled:text-indigo-800 disabled:hover:text-indigo-800',
  purple: 'text-purple-500 hover:text-purple-700 disabled:text-purple-800 disabled:hover:text-purple-800',
  pink: 'text-pink-500 hover:text-pink-700 disabled:text-pink-800 disabled:hover:text-pink-800',
});

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>((props: ButtonProps, ref: React.ForwardedRef<HTMLButtonElement>) => {
  const {variant, link, block, ...buttonProps} = props;
  let buttonVariant = variant;

  if (!BLOCK_VARIANTS[buttonVariant]) {
    // invalid variant supplied - reset to gray
    buttonVariant = 'gray';
  }

  const _base = link ? BASE_FOR_LINK : BASE_FOR_BLOCK;
  const _variant = (link ? LINK_VARIANTS : BLOCK_VARIANTS)[buttonVariant];

  return (
    <button {...buttonProps} ref={ref} className={clsx(buttonProps.className, BUTTON_BASE, _base, _variant, block && 'block w-full')}>{props.children}</button>
  );
});

Button.displayName = 'Button';
export default Button;
