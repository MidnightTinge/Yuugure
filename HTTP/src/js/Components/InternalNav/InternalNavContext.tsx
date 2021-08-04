import namedContext from '../../classes/NamedContext';

type InternalNavContextProps = {
  path: string;
}

export const InternalNavContext = namedContext<InternalNavContextProps>('NavContext');
export default InternalNavContext;
