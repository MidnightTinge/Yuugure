import * as React from 'react';
import {render} from 'react-dom';
import {Provider} from 'react-redux';

import {BrowserRouter as Router, Route, Switch} from 'react-router-dom';
import PageRenderer from './Components/PageRenderer';
import Page404 from './Pages/404/Page404';
import PageIndex from './Pages/Index/PageIndex';
import PageLogin from './Pages/Login/PageLogin';
import PageProfile from './Pages/Profile/PageProfile';
import PageRegister from './Pages/Register/PageRegister';
import PageUpload from './Pages/Upload/PageUpload';
import PageView from './Pages/View/PageView';
import {authStateStore} from './Stores/AuthStore';

function App() {
  return (
    <Provider store={authStateStore}>
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
          <Route path="*">
            <PageRenderer active="404">
              <Page404/>
            </PageRenderer>
          </Route>
        </Switch>
      </Router>
    </Provider>
  );
}

(() => {
  render(
    <App/>,
    document.getElementById('app'),
  );
})();
