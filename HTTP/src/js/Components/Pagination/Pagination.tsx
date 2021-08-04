import {mdiPageFirst, mdiPageLast} from '@mdi/js';
import Icon from '@mdi/react';
import clsx from 'clsx';
import * as React from 'react';
import {useMemo} from 'react';

export type PaginationProps = {
  current: number;
  max: number;
  urlFormat: string | ((page: number) => string);
  onNav?: (page: number) => void;
};

const leftIcon = (<Icon path={mdiPageFirst} size={1}/>);
const rightIcon = (<Icon path={mdiPageLast} size={1}/>);

const leftDisabled = (<button className="Floating disabled" disabled>{leftIcon}</button>);
const rightDisabled = (<button className="Floating disabled" disabled>{rightIcon}</button>);

function defaultFormatter(prefix: string) {
  return (page: number) => `${prefix}&page=${page}`;
}

export const Pagination: React.FC<PaginationProps> = (props: PaginationProps) => {
  const formatter: ((page: number) => string) = typeof props.urlFormat !== 'function' ? defaultFormatter(props.urlFormat) : props.urlFormat;

  function makePaginationUrl(page: number) {
    return formatter(page);
  }

  function makeNavigator(page: number) {
    return (e: React.MouseEvent<any>) => {
      e.preventDefault();
      if (typeof props.onNav === 'function') {
        props.onNav(page);
      }
    };
  }

  // Pagination rendering props
  const curPage = props.current;
  const lastPage = props.max;

  const pgExtend = lastPage > 10;
  const stepStart = curPage < 10 ? 1 : curPage - 4;
  const stepEnd = curPage < 10 ? (Math.min(lastPage, 10)) : (Math.min(curPage + 5, lastPage));

  const onFirstPage = curPage <= 1;
  const onLastPage = curPage >= lastPage;

  const leftFloat = onFirstPage ? leftDisabled : (<a href={makePaginationUrl(1)} onClick={makeNavigator(1)} className="Floating" aria-label="First Page">{leftIcon}</a>);
  const rightFloat = onLastPage ? rightDisabled : (<a href={makePaginationUrl(lastPage)} onClick={makeNavigator(lastPage)} className="Floating" aria-label="Last Page">{rightIcon}</a>);

  const pages = useMemo(() => {
    const pages = [];

    for (let i = stepStart; i <= stepEnd; i++) {
      pages.push(
        <a href={makePaginationUrl(i)} key={i} className={clsx('Page', curPage === i && 'active')} onClick={makeNavigator(i)} aria-label={`Page ${i}`}>{i}</a>,
      );
    }

    return pages;
  }, [props.current, props.max]);

  return (
    <div className="Pagination" role="navigation">
      {pgExtend ? leftFloat : null}
      <div className="Pages">
        {pages}
      </div>
      {pgExtend ? rightFloat : null}
    </div>
  );
};

export default Pagination;
