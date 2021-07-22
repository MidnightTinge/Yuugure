import {mdiFilePlusOutline} from '@mdi/js';
import Icon from '@mdi/react';
import clsx from 'clsx';
import * as React from 'react';
import Dropzone from 'react-dropzone';
import Util from '../classes/Util';

type FileDragDropProps = {
  className?: string;
  onFile?: (file: File) => void;
}

type FileDragDropState = {
  file: File
}

export default class FileDragDrop extends React.Component<FileDragDropProps, FileDragDropState> {
  wrapper: React.RefObject<HTMLDivElement>;
  inputId: string;

  constructor(props: FileDragDropProps) {
    super(props);
    this.state = {
      file: null,
    };
    this.wrapper = React.createRef();
    this.inputId = Util.mkid();
  }

  handleDrop(files: File[]) {
    this.setState({
      file: files[0],
    });
    if (typeof this.props.onFile === 'function') {
      this.props.onFile(files[0]);
    }
  }

  render() {
    return (
      <Dropzone onDrop={this.handleDrop.bind(this)}>
        {({getRootProps, getInputProps, isDragActive}) => (
          <div {...getRootProps({className: clsx('inline-block select-none cursor-pointer flex flex-col items-center justify-center min-w-48 min-h-20 py-1.5 rounded-lg bg-gray-100 border border-gray-200 shadow-md text-gray-500 hover:shadow-lg hover:text-gray-400', this.props.className)})}>
            <input {...getInputProps({className: 'sr-only'})}/>
            <Icon path={mdiFilePlusOutline} size="2rem"/>
            {this.state.file != null ? (
              <span className="text-green-600 font-medium">{this.state.file.name}</span>
            ) : null}
            <p className="text-center text-sm">{isDragActive ? 'Drop the file to accept' : 'Drag a file or click to browse'}</p>
          </div>
        )}
      </Dropzone>
    );
  }
}
