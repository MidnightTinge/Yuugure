import * as React from 'react';
import {Alert} from './Alert/Alert';
import AlertRenderer, {AlertClosed, AlertCloseRequest} from './Alert/AlertRenderer';

type AlertContainerProps = {
  alerts: Alert[];
  onCloseRequest: AlertCloseRequest;
  onClose: AlertClosed;
}

type AlertContainerState = {
  //
}

export default function AlertContainer({alerts, onClose: closed, onCloseRequest: closeRequested}: AlertContainerProps) {
  let onCloseRequest: AlertCloseRequest = typeof closeRequested === 'function' ? closeRequested : () => undefined;
  let onClose: AlertClosed = typeof closed === 'function' ? closed : () => undefined;

  return (
    <div className="AlertContainer">
      {alerts.map(alert => (<AlertRenderer alert={alert} key={alert.id} onCloseRequest={onCloseRequest} onClosed={onClose}/>))}
    </div>
  );
}
