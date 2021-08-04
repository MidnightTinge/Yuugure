import namedContext from '../../classes/NamedContext';
import {CloseSource} from './Modal';

type ModalContextProps = {
  closeButton: boolean;
  onCloseRequest: (cs: CloseSource) => void;
}

export const ModalContext = namedContext<ModalContextProps>('ModalContext');
export default ModalContext;
