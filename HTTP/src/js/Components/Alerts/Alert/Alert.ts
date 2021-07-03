export enum AlertType {
  INFO = 'info',
  ERROR = 'error',
  WARNING = 'warning',
}

export type Alert = {
  id: string;
  show: boolean;
  type?: AlertType,
  dismissable?: boolean;
  header?: import('react').ReactFragment;
  body: import('react').ReactFragment;
}
