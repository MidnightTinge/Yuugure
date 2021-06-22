import * as React from 'react';
import Util from '../../classes/Util';
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
    <div className={Util.joinedClassName('ListGroup', className)} {...elProps}>
      {children}
    </div>
  );
};
ListGroup.Item = ListGroupItem;

export default ListGroup;
