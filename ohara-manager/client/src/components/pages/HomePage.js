import React from 'react';
import DocumentTitle from 'react-document-title';

import { AppWrapper } from '../common/Layout';
import { HOME } from '../../constants/documentTitles';

class HomePage extends React.Component {
  render() {
    return (
      <DocumentTitle title={HOME}>
        <AppWrapper title="Ohara home" />
      </DocumentTitle>
    );
  }
}

export default HomePage;
