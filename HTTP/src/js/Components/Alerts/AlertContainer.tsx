import * as React from 'react';
import {Alert} from './Alert/Alert';
import AlertRenderer, {AlertClosed, AlertCloseRequest} from './Alert/AlertRenderer';

type AlertContainerProps = {
  alerts: Alert[];
  onCloseRequest: AlertCloseRequest;
  onClose: AlertClosed;
}

export const AlertContainer: React.FC<AlertContainerProps> = ({alerts, onClose: closed, onCloseRequest: closeRequested}: AlertContainerProps) => {
  const onCloseRequest: AlertCloseRequest = typeof closeRequested === 'function' ? closeRequested : () => undefined;
  const onClose: AlertClosed = typeof closed === 'function' ? closed : () => undefined;

  return (
    <div className="AlertContainer">
      {alerts.map(alert => (<AlertRenderer alert={alert} key={alert.id} onCloseRequest={onCloseRequest} onClosed={onClose}/>))}
    </div>
  );
};

export default AlertContainer;
