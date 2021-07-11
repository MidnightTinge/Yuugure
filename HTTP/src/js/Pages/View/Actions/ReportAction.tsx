import * as React from 'react';
import {useState} from 'react';
import Button from '../../../Components/Button';
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
      <Button variant="yellow" onClick={handleReport}><i className="fas fa-bullhorn mr-1" aria-hidden={true}/>Report</Button>
    </>
  );
}
