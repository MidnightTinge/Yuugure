import * as React from 'react';
import {useMemo} from 'react';
import {render} from 'react-dom';
import {QueryClient, QueryClientProvider} from 'react-query';

import {BrowserRouter as Router, Route, Switch} from 'react-router-dom';
import AlertsProvider from './Components/Alerts/AlertsProvider';
import PageRenderer from './Components/PageRenderer';
import AuthStateProvider from './Context/AuthStateProvider';
import WebSocketProvider from './Context/WebSocketProvider';
import Page404 from './Pages/404/Page404';
import PageIndex from './Pages/Index/PageIndex';
import PageLogin from './Pages/Login/PageLogin';
import PageProfile from './Pages/Profile/PageProfile';
import PageRegister from './Pages/Register/PageRegister';
import PageSearch from './Pages/Search/PageSearch';
import PageUpload from './Pages/Upload/PageUpload';
import PageView from './Pages/View/PageView';

function App() {
  const queryClient = useMemo(() => {
    return new QueryClient({
      defaultOptions: {
        queries: {
          refetchOnWindowFocus: false,
          refetchOnMount: true,
        },
      },
    });
  }, []);

  return (
    <AlertsProvider>
      <AuthStateProvider>
        <WebSocketProvider>
          <QueryClientProvider client={queryClient}>
            <Router>
              <Switch>
                <Route path="/auth/login">
                  <PageRenderer active="login">
                    <PageLogin/>
                  </PageRenderer>
                </Route>
                <Route path="/auth/register">
                  <PageRenderer active="register">
                    <PageRegister/>
                  </PageRenderer>
                </Route>
                <Route path="/upload">
                  <PageRenderer active="upload" authControlled>
                    <PageUpload/>
                  </PageRenderer>
                </Route>
                <Route path="/" exact>
                  <PageRenderer active="index">
                    <PageIndex/>
                  </PageRenderer>
                </Route>
                <Route path="/view/:uploadId">
                  <PageRenderer active="view">
                    <PageView/>
                  </PageRenderer>
                </Route>
                <Route path="/user/:accountId">
                  <PageRenderer active="profile">
                    <PageProfile/>
                  </PageRenderer>
                </Route>
                <Route path="/profile">
                  <PageRenderer active="profile">
                    <PageProfile self={true}/>
                  </PageRenderer>
                </Route>
                <Route path="/search">
                  <PageRenderer active="search">
                    <PageSearch/>
                  </PageRenderer>
                </Route>
                <Route path="*">
                  <PageRenderer active="404">
                    <Page404/>
                  </PageRenderer>
                </Route>
              </Switch>
            </Router>
          </QueryClientProvider>
        </WebSocketProvider>
      </AuthStateProvider>
    </AlertsProvider>
  );
}

(() => {
  render(
    <App/>,
    document.getElementById('app'),
  );
})();
