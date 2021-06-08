import * as React from 'react';
import {render} from 'react-dom';
import PageRenderer from '../Components/PageRenderer/PageRenderer';
import PageRegister from '../Pages/Register/PageRegister';

(() => {
  render(
    <PageRenderer active="register">
      <PageRegister/>
    </PageRenderer>,
    document.getElementById('app'),
  );
})();
