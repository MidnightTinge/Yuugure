import * as React from 'react';
import {useMemo, useState} from 'react';
import Util from '../../classes/Util';
import Modal from '../Modal/Modal';

export type DangerousConfirmModalProps = {
  confirmPhrase: string;
  onAction: (confirmed: boolean) => void;
  show: boolean;
  children?: React.ReactFragment;
  title?: string;
  confirmText?: string;
  cancelText?: string;
};

export default function DangerousConfirmModal(props: DangerousConfirmModalProps) {
  const [value, setValue] = useState<string>('');

  const phraseMatches = useMemo(() => value.trim() === props.confirmPhrase.trim(), [value]);
  const id = useMemo(() => Util.mkid(), []);

  function handleConfirm() {
    if (typeof props.onAction === 'function') {
      props.onAction(true);
    }
  }

  function handleCancel() {
    if (typeof props.onAction === 'function') {
      props.onAction(false);
    }
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (phraseMatches) {
      handleConfirm();
    }
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setValue(e.target.value);
  }

  function handleKeyUp(e: React.KeyboardEvent<HTMLInputElement>) {
    setValue(e.currentTarget.value);
  }

  function handleModalClosed() {
    setValue('');
  }

  return (
    <Modal onCloseRequest={handleCancel} show={props.show} onClose={handleModalClosed} closeButton>
      <Modal.Header>{props.title || 'Confirm'}</Modal.Header>
      <Modal.Body>
        {props.children}
        <form method="GET" action="#" onSubmit={handleSubmit}>
          <div>
            <p className="block">Please type <span className="font-bold italic font-mono cursor-text">{props.confirmPhrase}</span> to confirm.</p>
            <input type="text" id={id} onChange={handleChange} defaultValue={value} placeholder={props.confirmPhrase} className="block w-full rounded-md border border-gray-300 placeholder-gray-400 hover:bg-gray-50 shadow focus:border-gray-500 focus:ring focus:ring-gray-400 focus:ring-opacity-50 disabled:cursor-not-allowed disabled:bg-gray-200" required/>
          </div>
          <div className="mt-2 text-right">
            <button type="button" className="px-2 rounded bg-gray-200 border border-gray-300 hover:bg-gray-300 hover:border-gray-400 disabled:bg-gray-100 disabled:border-gray-200 disabled:text-gray-400 mr-2 disabled:cursor-not-allowed" onClick={handleCancel}>{props.cancelText || 'Cancel'}</button>
            <button type="submit" className="px-2 rounded bg-blue-300 border border-blue-400 hover:bg-blue-400 hover:border-blue-500 disabled:bg-blue-100 disabled:border-blue-200 disabled:text-gray-500 disabled:cursor-not-allowed" disabled={!phraseMatches}>{props.confirmText || 'Confirm'}</button>
          </div>
        </form>
      </Modal.Body>
    </Modal>
  );
}
