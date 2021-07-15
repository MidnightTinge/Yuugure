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

export default function RatingGroup(props: RatingGroupProps) {
  return (
    <div className="w-full">
      <RadioGroup value={props.rating} onChange={props.onChange}>
        <RadioGroup.Label className="mb-2 text-sm font-medium text-gray-800">Rating</RadioGroup.Label>
        <div className="space-y-3 px-2">
          <RadioGroup.Option value="safe" className={({active, checked}: OptionRenderPropArg) => clsx(getBaseClass(active, checked), checked && 'bg-green-100 border-green-200')}>
            {({checked}: OptionRenderPropArg) => (
              <div className="flex items-center justify-between w-full">
                <div className="flex items-center">
                  <div className="text-sm">
                    <RadioGroup.Label as="p" className={clsx('font-medium', checked && 'text-green-700')}>
                      Safe
                    </RadioGroup.Label>
                    <RadioGroup.Description as="span" className={clsx('inline')}>
                      Viewable by all ages.
                    </RadioGroup.Description>
                  </div>
                </div>
                {checked && (
                  <div className="flex-shrink-0 text-green-900">
                    <Icon path={mdiCheckCircle} size="2rem"/>
                  </div>
                )}
              </div>
            )}
          </RadioGroup.Option>
          <RadioGroup.Option value="questionable" className={({active, checked}: OptionRenderPropArg) => clsx(getBaseClass(active, checked), checked && 'bg-yellow-100 border-yellow-200')}>
            {({checked}: OptionRenderPropArg) => (
              <div className="flex items-center justify-between w-full">
                <div className="flex items-center">
                  <div className="text-sm">
                    <RadioGroup.Label as="p" className={clsx('font-medium', checked && 'text-yellow-700')}>
                      Questionable
                    </RadioGroup.Label>
                    <RadioGroup.Description as="span" className={clsx('inline')}>
                      Potentially lewd but nothing explicit.
                    </RadioGroup.Description>
                  </div>
                </div>
                {checked && (
                  <div className="flex-shrink-0 text-yellow-900">
                    <Icon path={mdiCheckCircle} size="2rem"/>
                  </div>
                )}
              </div>
            )}
          </RadioGroup.Option>
          <RadioGroup.Option value="explicit" className={({active, checked}: OptionRenderPropArg) => clsx(getBaseClass(active, checked), checked && 'bg-red-100 border border-red-200')}>
            {({checked}: OptionRenderPropArg) => (
              <div className="flex items-center justify-between w-full">
                <div className="flex items-center">
                  <div className="text-sm">
                    <RadioGroup.Label as="p" className={clsx('font-medium', checked && 'text-red-700')}>
                      Explicit
                    </RadioGroup.Label>
                    <RadioGroup.Description as="span" className={clsx('inline')}>
                      Sexually explicit/NSFW.
                    </RadioGroup.Description>
                  </div>
                </div>
                {checked && (
                  <div className="flex-shrink-0 text-red-900">
                    <Icon path={mdiCheckCircle} size="2rem"/>
                  </div>
                )}
              </div>
            )}
          </RadioGroup.Option>
        </div>
      </RadioGroup>
    </div>
  );
}

function getBaseClass(active: boolean, checked: boolean) {
  return clsx('bg-gray-100 border border-gray-200 relative rounded-lg shadow px-5 py-4 cursor-pointer flex focus:outline-none', checked && 'shadow-md', (checked || active) && 'ring-2 ring-offset-2 ring-white ring-offset-gray-100 ring-opacity-60');
}
