import namedContext from '../../classes/NamedContext';

type InternalNavContextProps = {
  path: string;
}

export const CreateNavContext = (props?: InternalNavContextProps) => {
  return namedContext('NavContext', props);
};

export const InternalNavContext = CreateNavContext();
export default InternalNavContext;
