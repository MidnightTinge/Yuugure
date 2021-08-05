import {mdiEyeOff} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import {useState} from 'react';
import KY from '../../../classes/KY';
import RouterResponseConsumer from '../../../classes/RouterResponseConsumer';
import ToggleSwitch from '../../../Components/ToggleSwitch/ToggleSwitch';

export type PrivateActionProps = {
  upload: RenderableUpload;
  checked: boolean;
};

export const PrivateAction: React.FC<PrivateActionProps> = ({upload, checked}: PrivateActionProps) => {
  const [posting, setPosting] = useState(false);

  function handleChange(checked: boolean) {
    setPosting(true);
    KY.patch(`/api/upload/${upload.upload.id}/private`, {
      body: new URLSearchParams({
        private: String(checked),
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
    <ToggleSwitch checked={checked} onChange={handleChange} loading={posting}><Icon path={mdiEyeOff} size={1} className="mr-1 relative bottom-px inline-block"/>Private</ToggleSwitch>
  );
};

export default PrivateAction;
