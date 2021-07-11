import {mdiClose} from '@mdi/js';
import Icon from '@mdi/react';
import * as React from 'react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {animate as Animate} from 'velocity-animate';
import useDelayUnmount from '../../../Hooks/useDelayUnmount';
import {Alert, AlertType} from './Alert';

export type AlertCloseRequest = (alert: Alert) => void;
export type AlertClosed = (alert: Alert) => void;

export type AlertProps = {
  alert: Alert,
  onCloseRequest: AlertCloseRequest,
  onClosed: AlertClosed;
}

type AlertColors = {
  header: string;
  body: string;
  border: string;
}

const ANIM_IN_LEN = 650;
const ANIM_OUT_LEN = 200;

function animateIn(el: HTMLDivElement) {
  el.style.right = '-34vw';
  el.style.opacity = '0.1';
  el.style.display = null;

  // forcefed values are passed as a second or third item in the array.
  // [1, 0] means we animate from 0 to 1.
  Animate(el, {
    right: ['0vw', '-34vw'],
    opacity: [0.8, 0.1],
  }, {
    duration: ANIM_IN_LEN,
    easing: [1, .02, .57, 1],
    complete: () => {
      el.style.right = null;
      el.style.opacity = null;
    },
  });
}

function animateOut(el: HTMLDivElement) {
  el.style.right = '0vw';
  el.style.opacity = '0.8';
  el.style.display = null;

  // forcefed values are passed as a second or third item in the array.
  // [1, 0] means we animate from 0 to 1.
  Animate(el, {
    right: ['-34vw', '0vw'],
    opacity: [0.1, 0.8],
  }, {
    duration: ANIM_OUT_LEN,
    easing: [1, .02, .57, 1],
    complete: () => {
      el.style.display = 'none';
    },
  });
}

export default function AlertRenderer({alert, onCloseRequest: closeRequest, onClosed: closed}: AlertProps) {
  const onCloseRequest: AlertCloseRequest = typeof closeRequest === 'function' ? closeRequest : () => undefined;
  const onClosed: AlertClosed = typeof closed === 'function' ? closed : () => undefined;

  const animated = useRef<boolean>(false);
  const [elAlert, setElAlert] = useState<HTMLDivElement>(null);
  const colors = useMemo(() => getColors(alert), [alert.type]);
  const render = useDelayUnmount(alert.show, ANIM_OUT_LEN + 10);

  useEffect(function mounted() {
    if (elAlert != null && !animated.current) {
      animated.current = true;
      animateIn(elAlert);
    } else if (elAlert == null && animated.current) {
      onClosed(alert);
    }
  }, [elAlert]);

  useEffect(() => {
    if (elAlert != null && animated.current) {
      animateOut(elAlert);
    }
  }, [alert.show]);

  function handleCloseClick() {
    onCloseRequest(alert);
  }

  return render ? (
    <div className="Alert" style={{display: 'none'}} ref={setElAlert} id={alert.id} role={alert.dismissable ? 'alertdialog' : 'alert'} aria-labelledby={`h-${alert.id}`} aria-describedby={`b-${alert.id}`}>
      <div className={`AlertHeader ${colors.header} ${colors.border}`} id={`h-${alert.id}`}>
        {alert.dismissable ? <span className="pr-4">{alert.header}</span> : alert.header}
        {alert.dismissable ? (
          <button className="CloseButton" aria-label="Close" onClick={handleCloseClick} autoFocus><Icon path={mdiClose} size={1} className="inline-block"/></button>
        ) : null}
      </div>
      <div className={`AlertBody ${colors.body} ${colors.border}`} id={`b-${alert.id}`}>
        {alert.body}
      </div>
    </div>
  ) : null;
};

function getColors(alert: Alert): AlertColors {
  switch (alert.type) {
    case AlertType.INFO: {
      return {
        header: 'bg-blue-200',
        body: 'bg-blue-100',
        border: 'border-blue-300',
      };
    }
    case AlertType.ERROR: {
      return {
        header: 'bg-red-200',
        body: 'bg-red-100',
        border: 'border-red-300',
      };
    }
    case AlertType.WARNING: {
      return {
        header: 'bg-yellow-200',
        body: 'bg-yellow-100',
        border: 'border-yellow-300',
      };
    }
    case AlertType.SUCCESS: {
      return {
        header: 'bg-green-200',
        body: 'bg-green-100',
        border: 'border-green-300',
      };
    }
    default: {
      return {
        header: 'bg-gray-200',
        body: 'bg-gray-100',
        border: 'border-gray-300',
      };
    }
  }
}
