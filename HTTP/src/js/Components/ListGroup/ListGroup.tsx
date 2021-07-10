import clsx from 'clsx';
import * as React from 'react';
import ListGroupItem, {ListGroupItemProps} from './ListGroupItem';

interface IListGroupComposition {
  Item: React.FC<ListGroupItemProps>;
}

export type ListGroupProps = React.HTMLAttributes<{
  children: React.ReactFragment
}>;

export const ListGroup: React.FC<ListGroupProps> & IListGroupComposition = (props: ListGroupProps) => {
  const {children, className, ...elProps} = props;

  return (
    <div className={clsx('ListGroup', className)} {...elProps}>
      {children}
    </div>
  );
};
ListGroup.Item = ListGroupItem;

export default ListGroup;
