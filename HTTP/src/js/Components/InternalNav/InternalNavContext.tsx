import * as React from 'react';

type InternalNavContextProps = {
  path: string;
}

// need the `extends unknown` to fix an issue with TypeScript detecting it as JSX
export const CreateNavContext = (defaultValue?: InternalNavContextProps) => {
  const context = React.createContext(defaultValue);
  context.displayName = 'NavContext';

  return context;
};

export const InternalNavContext = CreateNavContext();
export default InternalNavContext;
