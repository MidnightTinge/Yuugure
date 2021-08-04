import {mdiAlertOctagon} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import {useState} from 'react';
import Button from '../../../Components/Button';
import {CloseSource} from '../../../Components/Modal/Modal';
import ReportModal from '../../../Components/modals/ReportModal';

export type ReportActionProps = {
  upload: RenderableUpload;
};

export const ReportAction: React.FC<ReportActionProps> = (props: ReportActionProps) => {
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
      <Button variant="yellow" onClick={handleReport}><Icon path={mdiAlertOctagon} size={1} className="mr-1 inline-block relative bottom-px"/>Report</Button>
    </>
  );
};

export default ReportAction;
