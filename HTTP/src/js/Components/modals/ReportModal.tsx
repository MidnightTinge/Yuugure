import * as React from 'react';
import {useMemo, useState} from 'react';
import KY from '../../classes/KY';
import RouterResponseConsumer from '../../classes/RouterResponseConsumer';
import Util from '../../classes/Util';
import Button from '../Button';
import Modal, {CloseSource} from '../Modal/Modal';
import Spinner from '../Spinner';

export type ReportModalProps = & {
  show?: boolean;
  targetType: string;
  targetId: number;
  onReportSent?: (report: ReportResponse) => void;
  onCloseRequest: (cs: CloseSource, posting: boolean) => void;
};

export default function ReportModal(props: ReportModalProps) {
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState<string>(null);
  const [reported, setReported] = useState<boolean>(false);

  const txtReason = React.useRef<HTMLTextAreaElement>();
  const id = useMemo(() => Util.mkid(), []);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setPosting(true);
    KY.post(`/api/${props.targetType}/${props.targetId}/report`, {
      body: Util.formatUrlEncodedBody({
        reason: txtReason.current.value,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer<ReportResponse>(data);
      if (consumed.success) {
        let [report] = consumed.data;
        setError(null);
        setReported(true);
        if (typeof props.onReportSent === 'function') {
          props.onReportSent(report);
        }
      } else {
        setError(consumed.message);
        setReported(false);
      }
    }).catch(err => {
      setError(err.toString());
      setReported(false);
    }).then(() => {
      setPosting(false);
    });
  }

  function handleClose() {
    setPosting(false);
    setReported(false);
    setError(null);
    if (txtReason.current != null) {
      txtReason.current.value = '';
    }
  }

  return (
    <Modal show={props.show} onCloseRequest={cs => props.onCloseRequest(cs, posting)} className="w-11/12 sm:w-6/12" closeButton={!posting} onClose={handleClose}>
      <Modal.Header>Report</Modal.Header>
      <Modal.Body>
        {reported ? (
          <div className="text-center">
            <i className="fas fa-check-circle text-green-500 fa-4x"/>
            <p className="text-green-800">Your report has been sent successfuly. You can close this dialog.</p>
          </div>
        ) : (
          <form method="post" action={`/api/${props.targetType}/${props.targetId}`} onSubmit={handleSubmit}>
            <div>
              <label htmlFor={`${id}`}>Reason:</label>
              <textarea ref={txtReason} id={id} rows={5} disabled={posting} name="reason" className={`block w-full rounded-md shadow border bg-gray-50 border-gray-300 hover:bg-gray-100 disabled:cursor-not-allowed disabled:bg-gray-200`} placeholder="Breaks rule #3" required/>
            </div>
            <div className="text-right mt-3">
              <Button type="submit" variant="green" disabled={posting}>
                {posting ? (
                  <><Spinner/> Sending...</>
                ) : `Send`}
              </Button>
            </div>
          </form>
        )}
        {error ? (
          <div className="text-center">
            <p className="text-red-800 whitespace-pre-wrap">{error}</p>
          </div>
        ) : null}
      </Modal.Body>
    </Modal>
  );
}
