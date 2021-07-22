import {RadioGroup} from '@headlessui/react';
import {mdiCheckCircle} from '@mdi/js';
import Icon from '@mdi/react';
import clsx from 'clsx';
import * as React from 'react';

interface OptionRenderPropArg {
  checked: boolean;
  active: boolean;
  disabled: boolean;
}

export type RatingGroupProps = {
  rating: string;
  onChange: (str: string) => void;
};

const ratings = [
  {
    header: 'Safe',
    footer: 'Viewable by all ages.',

    bg: 'bg-green-100',
    border: 'border-green-200',
    label: 'text-green-700',
    icon: 'text-green-900',
  },
  {
    header: 'Questionable',
    footer: 'Potentially lewd but nothing explicit.',

    bg: 'bg-yellow-100',
    border: 'border-yellow-200',
    label: 'text-yellow-700',
    icon: 'text-yellow-900',
  },
  {
    header: 'Explicit',
    footer: 'Sexually explicit/NSFW.',

    bg: 'bg-red-100',
    border: 'border-red-200',
    label: 'text-red-700',
    icon: 'text-red-900',
  },
];

export default function RatingGroup(props: RatingGroupProps) {
  return (
    <div className="w-full">
      <RadioGroup value={props.rating} onChange={props.onChange}>
        <RadioGroup.Label className="pb-2 text-sm font-medium text-gray-800">Rating</RadioGroup.Label>
        <div className="space-y-3 px-2">
          {
            ratings.map((rating, idx) => (
              <RadioGroup.Option key={rating.header} value={rating.header.toLowerCase()} className={({active, checked}: OptionRenderPropArg) => clsx(getBaseClass(active, checked), checked && rating.bg, checked && rating.border)}>
                {({checked}: OptionRenderPropArg) => (
                  <div className="flex items-center justify-between w-full">
                    <div className="flex items-center">
                      <div className="">
                        <RadioGroup.Label as="p" className={clsx('font-medium', checked && rating.label)}>
                          {rating.header}
                        </RadioGroup.Label>
                        <RadioGroup.Description as="span" className={clsx('inline text-sm')}>
                          {rating.footer}
                        </RadioGroup.Description>
                      </div>
                    </div>
                    {checked && (
                      <div className={clsx('flex-shrink-0', rating.icon)}>
                        <Icon path={mdiCheckCircle} size="2rem"/>
                      </div>
                    )}
                  </div>
                )}
              </RadioGroup.Option>
            ))
          }
        </div>
      </RadioGroup>
    </div>
  );
}

function getBaseClass(active: boolean, checked: boolean) {
  return clsx('bg-white border border-gray-200 relative rounded-lg shadow-md px-5 py-4 cursor-pointer flex focus:outline-none hover:shadow-lg transition-all', checked && 'shadow-md');
}
