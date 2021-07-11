import * as React from 'react';
import {useRef} from 'react';
import useId from '../../Hooks/useId';
import Button from '../Button';
import Modal from '../Modal/Modal';

export type InputModalProps = {
  onComplete: (input: string) => void;
  show: boolean;
  children: React.ReactNode
};

export default function InputModal(props: InputModalProps) {
  const txtInput = useRef<HTMLTextAreaElement>(null);
  const id = useId();

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    if (txtInput.current.value.trim().length > 0) {
      props.onComplete(txtInput.current.value);
    }
  }

  function handleCloseRequest() {
    props.onComplete(null);
  }

  return (
    <Modal show={props.show} onCloseRequest={handleCloseRequest} closeButton>
      <Modal.Header>Input</Modal.Header>
      <Modal.Body>
        {props.children}

        <form action="#" method="GET" onSubmit={handleSubmit}>
          <label htmlFor={id} id={`lbl-${id}`}>Input:</label>
          <textarea ref={txtInput} className="block w-full rounded-md border border-gray-300 hover:bg-gray-50 shadow focus:border-gray-500 focus:ring focus:ring-gray-400 focus:ring-opacity-50 disabled:cursor-not-allowed disabled:bg-gray-200" required/>
          <div className="mt-2 text-right">
            <Button type="button" variant="gray" className="mr-1" onClick={handleCloseRequest}>Cancel</Button>
            <Button type="submit" variant="green">Submit</Button>
          </div>
        </form>
      </Modal.Body>
    </Modal>
  );
}
