import * as React from 'react';
import {useMemo, useState} from 'react';
import Util from '../../classes/Util';
import Button from '../Button';
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
            <Button type="button" variant="gray" onClick={handleCancel}>{props.cancelText || 'Cancel'}</Button>
            <Button type="submit" variant="blue" disabled={!phraseMatches}>{props.confirmText || 'Confirm'}</Button>
          </div>
        </form>
      </Modal.Body>
    </Modal>
  );
}
