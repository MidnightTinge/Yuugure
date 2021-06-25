import * as React from 'react';
import {useMemo, useState} from 'react';
import Util from '../../classes/Util';
import {XHR} from '../../classes/XHR';
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
    XHR.for(`/api/${props.targetType}/${props.targetId}/report`).post(XHR.BODY_TYPE.FORM, {
      reason: txtReason.current.value,
    }).getJson<RouterResponse<ReportResponse>>().then(data => {
      if (data) {
        if (data.code === 200 && Array.isArray(data.data.ReportResponse) && data.data.ReportResponse.length > 0) {
          setReported(true);
          if (typeof props.onReportSent === 'function') {
            props.onReportSent(data.data.ReportResponse[0]);
          }
        } else if (data.code === 429) {
          setError(`You are doing that too often. Try again ${data.data && data.data.RateLimitResponse ? (`in ${data.data.RateLimitResponse[0].minimum_wait / 1e3 >> 0} seconds`) : 'later'}.`);
        } else {
          setError('Received an invalid response while trying to report. Please try again later.');
          console.error('Got invalid ReportResponse:', data);
          setReported(false);
        }
      } else {
        setError('An internal server error occurred. Please try again later.');
        console.error('Got invalid ReportResponse:', data);
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
              <button type="submit" className="px-2 bg-green-400 border border-green-500 rounded-md text-white hover:bg-green-500 hover:border-green-600 disabled:bg-green-200 disabled:border-green-300 disabled:cursor-not-allowed" disabled={posting}>
                {posting ? (
                  <><Spinner/> Sending...</>
                ) : `Send`}
              </button>
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
