import * as React from 'react';
import Util from '../../classes/Util';

export type ListGroupItemProps = React.HTMLAttributes<HTMLButtonElement> & {
  children: React.ReactFragment;
  active?: boolean;
};

const ListGroupItem = React.forwardRef((props: ListGroupItemProps, ref: React.ForwardedRef<HTMLButtonElement>) => {
  const {children, active, className, ...btnProps} = props;

  return (
    <button ref={ref} className={`${Util.joinedClassName('ListGroupItem', className)}${active ? ' active' : ''}`} {...btnProps}>
      {children}
    </button>
  );
});
export default ListGroupItem;
