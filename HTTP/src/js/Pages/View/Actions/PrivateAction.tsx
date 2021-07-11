import * as React from 'react';
import {useState} from 'react';
import KY from '../../../classes/KY';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import Util from '../../../classes/Util';
import ToggleSwitch from '../../../Components/ToggleSwitch/ToggleSwitch';

export type PrivateActionProps = {
  upload: RenderableUpload;
  checked: boolean;
};

export default function PrivateAction({upload, checked}: PrivateActionProps) {
  const [posting, setPosting] = useState(false);

  function handleChange(checked: boolean) {
    setPosting(true);
    KY.patch(`/api/upload/${upload.upload.id}/private`, {
      body: Util.formatUrlEncodedBody({
        private: checked,
      }),
    }).json<RouterResponse>().then(data => {
      const consumed = RouterResponseConsumer(data);
      if (consumed.success) {
        // checked state will ultimately be overwritten upstream when the websocket triggers.
      } else {
        console.error('Failed to set private state.', {consumed, data});
      }
    }).catch(err => {
      console.error('Failed to update private state.', err);
    }).then(() => {
      setPosting(false);
    });
  }

  return (
    <ToggleSwitch checked={checked} onChange={handleChange} loading={posting}><i className="fas fa-eye-slash mr-1" aria-hidden={true}/>Private</ToggleSwitch>
  );
}
