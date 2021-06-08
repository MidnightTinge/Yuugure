import * as React from 'react';
import {render} from 'react-dom';
import PageRenderer from '../Components/PageRenderer/PageRenderer';
import PageLogin from '../Pages/Login/PageLogin';

(() => {
  render(
    <PageRenderer active="login">
      <PageLogin/>
    </PageRenderer>,
    document.getElementById('app'),
  );
})();
