import clsx from 'clsx';
import * as React from 'react';

export type ListGroupItemProps = React.HTMLAttributes<HTMLButtonElement> & {
  children: React.ReactFragment;
  active?: boolean;
};

const ListGroupItem = React.forwardRef((props: ListGroupItemProps, ref: React.ForwardedRef<HTMLButtonElement>) => {
  const {children, active, className, ...btnProps} = props;

  return (
    <button ref={ref} className={clsx('ListGroupItem', {className, active})} {...btnProps}>
      {children}
    </button>
  );
});
export default ListGroupItem;
