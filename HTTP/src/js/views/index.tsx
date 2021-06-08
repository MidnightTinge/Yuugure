import * as React from 'react';
import {render} from 'react-dom';
import PageRenderer from '../Components/PageRenderer/PageRenderer';
import PageIndex from '../Pages/Index/PageIndex';

(() => {
  render(
    <PageRenderer active="index">
      <PageIndex/>
    </PageRenderer>,
    document.getElementById('app'),
  );
})();
