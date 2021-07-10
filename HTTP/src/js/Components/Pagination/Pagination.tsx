import clsx from 'clsx';
import * as React from 'react';
import {useMemo} from 'react';

export type PaginationProps = {
  current: number;
  max: number;
  urlFormat: string | ((page: number) => string);
  onNav?: (page: number) => void;
};

const leftIcon = (<i className="fas fa-step-backward"/>);
const rightIcon = (<i className="fas fa-step-forward"/>);

const leftDisabled = (<button className="Floating disabled" disabled>{leftIcon}</button>);
const rightDisabled = (<button className="Floating disabled" disabled>{rightIcon}</button>);

function defaultFormatter(prefix: string) {
  return (page: number) => `${prefix}&page=${page}`;
}

export default function Pagination(props: PaginationProps) {
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
  let curPage = props.current;
  let lastPage = props.max;

  let pgExtend = lastPage > 10;
  let stepStart = curPage < 10 ? 1 : curPage - 4;
  let stepEnd = curPage < 10 ? (Math.min(lastPage, 10)) : (Math.min(curPage + 5, lastPage));

  let onFirstPage = curPage <= 1;
  let onLastPage = curPage >= lastPage;

  const leftFloat = onFirstPage ? leftDisabled : (<a href={makePaginationUrl(1)} onClick={makeNavigator(1)} className="Floating">{leftIcon}</a>);
  const rightFloat = onLastPage ? rightDisabled : (<a href={makePaginationUrl(lastPage)} onClick={makeNavigator(lastPage)} className="Floating">{rightIcon}</a>);

  const pages = useMemo(() => {
    let pages = [];

    for (let i = stepStart; i <= stepEnd; i++) {
      pages.push(
        <a href={makePaginationUrl(i)} key={i} className={clsx('Page', curPage === i && 'active')} onClick={makeNavigator(i)}>{i}</a>,
      );
    }

    return pages;
  }, [props.current, props.max]);

  return (
    <div className="Pagination">
      {pgExtend ? leftFloat : null}
      <div className="Pages">
        {pages}
      </div>
      {pgExtend ? rightFloat : null}
    </div>
  );
}
