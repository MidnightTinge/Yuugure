import * as React from 'react';
import {FormEvent, useMemo, useRef, useState} from 'react';
import Util from '../classes/Util';

export type FileInputProps = {
  onFiles: (e: FileList) => void;
  label: string;
  required?: true;
  id?: string;
  disabled?: boolean;
  invalid?: boolean;
  errorText?: string;
}

export default function FileInput(props: FileInputProps) {
  const _id = useMemo(() => props.id || Util.mkid(), []);
  const [fname, setFname] = useState<string>(null);

  const fiFile = useRef<HTMLInputElement>(null);

  function handleChange(e: FormEvent<HTMLInputElement>) {
    setFname(fiFile.current.files.length > 0 && fiFile.current.files[0] != null ? fiFile.current.files[0].name : null);
    props.onFiles(fiFile.current.files);
  }

  const border = props.invalid ? `border-red-400` : `border-gray-400`;
  const color = props.invalid ? `text-red-600` : `text-gray-500`;

  return (
    <div className="my-1">
      <label className="block">
        <span className="mb-1 block text-sm font-medium text-gray-800" id={`lbl-${_id}`}>{props.label}</span>
        <input ref={fiFile} id={_id} className="hidden opacity-0" type="file" aria-labelledby={`lbl-${_id}`} required={props.required === true} onChange={handleChange}/>
        <div className={`shadow ${border} rounded-md flex flex-row`}>
          <span className={`border ${border} bg-gray-300 text-gray-600 rounded-l-md block flex-shrink px-3 py-1 cursor-pointer hover:bg-gray-400 hover:text-gray-800 font-medium`}>Select File</span>
          <label htmlFor={_id} className={`block border border-l-0 ${border} ${color} flex-grow p-1 rounded-r-md italic pl-3`}>{fname || 'No file selected'}</label>
        </div>
      </label>
      {(props.invalid && props.errorText) ? (
        <span className="mt-0.5 ml-1 block text-sm text-red-500">{props.errorText}</span>
      ) : null}
    </div>
  );
}
