import namedContext from '../../classes/NamedContext';
import {CloseSource} from './Modal';

type ModalContextProps = {
  closeButton: boolean;
  onCloseRequest: (cs: CloseSource) => void;
}

export const CreateModalContext = (props?: ModalContextProps) => {
  return namedContext('ModalContext', props);
};

export const ModalContext = CreateModalContext();
export default ModalContext;
