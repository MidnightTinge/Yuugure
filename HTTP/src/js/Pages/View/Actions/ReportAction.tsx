import * as React from 'react';
import {useState} from 'react';
import {CloseSource} from '../../../Components/Modal/Modal';
import ReportModal from '../../../Components/modals/ReportModal';

export type ReportActionProps = {
  upload: RenderableUpload;
};

export default function ReportAction(props: ReportActionProps) {
  const [showModal, setShowModal] = useState(false);

  function handleReport() {
    setShowModal(true);
  }

  function handleCloseRequest(cs: CloseSource, posting: boolean) {
    if (!posting) {
      setShowModal(false);
    }
  }

  return (
    <>
      <ReportModal targetType="upload" targetId={props.upload.upload.id} onCloseRequest={handleCloseRequest} show={showModal}/>
      <button className="inline-block px-4 py-1 font-semibold text-gray-700 rounded border border-yellow-400 bg-yellow-300 hover:bg-yellow-400 disabled:cursor-not-allowed my-1" onClick={handleReport}><i className="fas fa-bullhorn mr-1" aria-hidden={true}/>Report</button>
    </>
  );
}
