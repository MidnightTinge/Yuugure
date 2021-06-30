import * as React from 'react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {useQuery} from 'react-query';
import {useLocation} from 'react-router';
import {useHistory} from 'react-router-dom';
import Util from '../../classes/Util';
import {XHR} from '../../classes/XHR';
import LoadingPing from '../../Components/LoadingPing';
import MediaPreview from '../../Components/MediaPreview/MediaPreview';
import Pagination from '../../Components/Pagination/Pagination';

type SearchState = {
  page: SearchPagination;
  uploads: RenderableUpload[];
}

export type PageSearchProps = {
  //
};

function parseLocation(): Record<string, any> {
  if (document.location.search) {
    return (document.location.search.startsWith('?') ? document.location.search.substring(1) : document.location.search)
      .split('&')
      .map(str => str.split('='))
      .reduce((map: Record<string, any>, val, idx, arr) => {
        map[decodeURIComponent(val[0])] = decodeURIComponent(val[1]);
        return map;
      }, {});
  }
  return {};
}

export default function PageSearch(props: PageSearchProps) {
  const history = useHistory();
  const location = useLocation();
  const [query, setQuery] = useState<string>(null);
  const [page, setPage] = useState<number>(1);

  const txtSearch = useRef<HTMLInputElement>(null);
  const txtSearchId = useMemo(() => Util.mkid(), []);

  async function runSearch(): Promise<SearchState> {
    if (query != null && query.trim().length > 0) {
      let {data, success, message} = await XHR.for(`/search?q=${encodeURIComponent(query)}&page=${encodeURIComponent(page)}`).get().getRouterResponse<SearchResult>();
      if (success) {
        const [result] = data;

        return {
          page: result.page,
          uploads: Util.mapBulkUploads(result.result),
        };
      } else {
        console.error('RouterResponse returned non-successful:', {message, data});
        throw new Error(message); // for react-query to catch. will bubble out of our local catch.
      }
    } else {
      return Promise.resolve({page: {current: 1, max: 1}, uploads: []});
    }
  }

  // Paginated react-query query to hit /search endpoint.
  const {data, error, isLoading, isSuccess, isError} = useQuery({
    queryKey: ['imagesearch', query, page],
    queryFn: runSearch,

    keepPreviousData: true,
    staleTime: 30e3,
    refetchOnMount: 'always',
    refetchOnReconnect: 'always',
  });

  // Parse location params on URL change, update query/page if necessary.
  // Fixes browser back/forward button navigation.
  useEffect(() => {
    const {page: p, q} = parseLocation();

    let _page, _query;
    if (p != null && !isNaN(p)) {
      _page = (p >> 0) || 1;
    }
    if (q != null && q.trim().length > 0) {
      _query = q;
    }

    if (_query != null) {
      if (_query !== query) {
        setQuery(_query);
      }
      if (_page !== page) {
        setPage(_page == null ? 1 : _page);
      }
    }
  }, [location]);

  // Update query/page from URL params on mount
  useEffect(function mounted() {
    if (document.location.search) {
      const {page, q} = parseLocation();
      let _page = 1, _query = null;

      if (page != null && !isNaN(page)) {
        _page = page >> 0 || 1;
      }
      if (q != null && q.trim().length > 0) {
        _query = q;
      }

      if (_query != null && _query.trim().length > 0) {
        setPage(_page);
        setQuery(_query);
      }
    }

    return function unmounted() {
      //
    };
  }, []);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setQuery(txtSearch.current.value);
    setPage(1);
    history.push(`/search?q=${encodeURIComponent(txtSearch.current.value)}&page=${encodeURIComponent(1)}`);
  }

  function handlePaginationNav(page: number) {
    setPage(page);
    history.push(`/search?q=${encodeURIComponent(query)}&page=${encodeURIComponent(page)}`);
  }

  return (
    query == null ? (
      <div className="w-8/12 md:w-11/12 mx-auto my-2 bg-gray-100 border border-gray-200 p-3 rounded-md shadow-lg">
        <form method="GET" action="/search" onSubmit={handleSubmit}>
          <input name="page" type="hidden" value={1}/>
          <div>
            <label htmlFor={txtSearchId} className="text-gray-500 font-semibold">Search</label>
            <input type="text" ref={txtSearch} id={txtSearchId} name="q" className="block w-full border border-gray-300 bg-gray-200 shadow-sm rounded-md"/>
          </div>
          <div className="text-right mt-3">
            <button className="inline-block border border-blue-400 bg-blue-300 px-4 py-1 rounded font-semibold text-white hover:bg-blue-400 disabled:cursor-not-allowed">Search</button>
          </div>
        </form>
      </div>
    ) : (
      <div className="grid grid-cols-12 gap-2 pt-2 pl-2 h-full max-w-screen overflow-x-hidden">
        <div className="col-span-4 md:col-span-2">
          <section className="rounded-md shadow">
            <form method="GET" action="/search" onSubmit={handleSubmit}>
              <input name="page" type="hidden" value={1}/>
              <div className="flex">
                <input type="text" ref={txtSearch} id={txtSearchId} defaultValue={query} placeholder="Search" className="bg-gray-100 border border-r-0 border-gray-300 rounded-l-md px-1.5 py-0.5 w-full placeholder-gray-400 focus:ring-0 focus-within:ring-0 focus:shadow-none focus-within:shadow-none focus:border-gray-300 focus-within:border-gray-300 focus:outline-none focus-within:outline-none active:outline-none"/>
                <button className="px-1.5 flex items-center justify-center bg-gray-200 border border-gray-300 rounded-r-md hover:bg-gray-300 focus:outline-none focus-within:outline-none active:outline-none"><i className="fas fa-search text-sm" aria-hidden={true}/><span className="sr-only">Submit</span></button>
              </div>
            </form>
          </section>
          <section className="mt-2">
            <div className="rounded bg-gray-100 border border-gray-200 shadow">
              <div className="py-0.5 text-center text-gray-500 border-b border-gray-200 font-medium">Blacklist settings</div>
              <div className="p-2">
                <p className="text-gray-400">placeholder</p>
              </div>
            </div>
          </section>
          <section className="mt-2">
            <div className="rounded bg-gray-100 border border-gray-200 shadow">
              <div className="py-0.5 text-center text-gray-500 border-b border-gray-200 font-medium">Tags on this page</div>
              <div className="p-2">
                <p className="text-gray-400">placeholder</p>
              </div>
            </div>
          </section>
        </div>
        <div className="col-span-8 md:col-span-10">
          <div className={isLoading ? 'h-full flex flex-col justify-center items-center' : 'm-1 p-3'}>
            {isLoading ? (<LoadingPing/>) : (
              isError ? (
                <p className="text-lg text-red-500 whitespace-pre-wrap">Search failed: {error}</p>
              ) : (
                data.uploads.length > 0 ? (
                  data.uploads.map(value => <MediaPreview upload={value} key={value.upload.id}/>)
                ) : (
                  <p>No results.</p>
                )
              )
            )}
          </div>
          {isSuccess ? (
            <div className="m-1 p-3">
              <Pagination current={page} max={data != null ? data.page.max : 1} urlFormat={`/search?q=${encodeURIComponent(query)}`} onNav={handlePaginationNav}/>
            </div>
          ) : null}
        </div>
      </div>
    )
  );
}
