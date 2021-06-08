import * as React from 'react';
import {render} from 'react-dom';
import PageRenderer from '../Components/PageRenderer/PageRenderer';
import PageUpload from '../Pages/Upload/PageUpload';

(() => {
  render(
    <PageRenderer active="upload">
      <PageUpload/>
    </PageRenderer>,
    document.getElementById('app'),
  );
})();
