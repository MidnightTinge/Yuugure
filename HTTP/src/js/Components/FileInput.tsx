import clsx from 'clsx';
import * as React from 'react';
import {FormEvent} from 'react';
import Util from '../classes/Util';

type FileInputProps = {
  onFiles: (e: FileList) => void;
  label: string;
  required?: true;
  id?: string;
  disabled?: boolean;
  invalid?: boolean;
  errorText?: string;
}

type FileInputState = {
  fname: string;
}

export default class FileInput extends React.Component<FileInputProps, FileInputState> {
  id: string;
  fiFile: React.RefObject<HTMLInputElement>;

  constructor(props: FileInputProps) {
    super(props);
    this.state = {
      fname: null,
    };
    this.id = Util.mkid();
    this.fiFile = React.createRef();
  }

  _onChange(e: FormEvent<HTMLInputElement>) {
    this.handleFiles(this.fiFile.current.files);
  }

  handleFiles(files?: FileList) {
    let fileName = null;
    if (files?.length > 0 && files[0].size > 0) {
      fileName = files[0].name || 'file';
    }
    this.setState({
      fname: fileName,
    });
  }

  render() {
    const border = this.props.invalid ? `border-red-400` : `border-gray-300`;
    const color = this.props.invalid ? `text-red-600` : `text-gray-500`;

    return (
      <div className="my-1">
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-gray-800" id={`lbl-${this.id}`}>{this.props.label}</span>
          <input ref={this.fiFile} id={this.id} className="sr-only" type="file" aria-labelledby={`lbl-${this.id}`} required={this.props.required === true} onChange={this._onChange.bind(this)}/>
          <div className={clsx(`rounded-md flex flex-row`)}>
            <span className={clsx(`font-medium border bg-gray-200 text-gray-800 rounded-l-md block flex-shrink px-3 py-1 cursor-pointer hover:bg-gray-300 hover:text-gray-900`, border)}>Select File</span>
            <label htmlFor={this.id} className={`block border border-l-0 ${border} ${color} flex-grow p-1 rounded-r-md italic pl-3`}>{this.state.fname || 'No file selected'}</label>
          </div>
        </label>
        {(this.props.invalid && this.props.errorText) ? (
          <span className="mt-0.5 ml-1 block text-sm text-red-500">{this.props.errorText}</span>
        ) : null}
      </div>
    );
  }
}
