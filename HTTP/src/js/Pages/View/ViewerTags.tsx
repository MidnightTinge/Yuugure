import * as React from 'react';
import {useContext, useMemo} from 'react';
import {useHistory} from 'react-router-dom';
import DetailsBox from '../../Components/DetailsBox';
import {PageViewContext} from './PageView';

export type ViewerTagsProps = {
  //
};

export const ViewerTags: React.FC<ViewerTagsProps> = () => {
  const history = useHistory();
  const {upload} = useContext(PageViewContext);

  const tags = useMemo(() => {
    if (upload == null) {
      return {user: [], system: []};
    }

    const user = [];
    const system = [];
    for (const tag of upload.tags) {
      if (tag.category === 'userland') {
        user.push(tag);
      } else {
        system.push(tag);
      }
    }

    user.sort((a, b) => a.name.localeCompare(b.name));
    system.sort((a, b) => a.name.localeCompare(b.name));

    return {user, system};
  }, [upload != null ? upload.tags : null]);

  function makeRedirector(to: string) {
    return (e: React.MouseEvent<HTMLAnchorElement>) => {
      e.preventDefault();
      history.push(to);
    };
  }

  return (
    <DetailsBox header="Tags">
      {tags.system.map(tag => (<div key={tag.id}><a href={`/search?q=${encodeURIComponent(`${tag.category}:${tag.name}`)}`} onClick={makeRedirector(`/search?q=${encodeURIComponent(`${tag.category}:${tag.name}`)}`)} className="whitespace-pre-wrap break-all text-gray-700 hover:underline hover:text-gray-500" data-tag={tag.id} data-category={tag.category} data-name={tag.name}>{tag.category}:{tag.name}</a></div>))}
      {tags.user.map(tag => (<div key={tag.id}><a href={`/search?q=${encodeURIComponent(tag.name)}`} onClick={makeRedirector(`/search?q=${encodeURIComponent(tag.name)}`)} className="whitespace-pre-wrap break-all text-gray-700 hover:underline hover:text-gray-500" data-tag={tag.id} data-category={tag.category} data-name={tag.name}>{tag.name}</a></div>))}
    </DetailsBox>
  );
};

export default ViewerTags;
