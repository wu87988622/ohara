import React from 'react';

import DocumentTitle from 'react-document-title';

import { AppWrapper } from 'common/Layout';
import { SERVICES } from 'constants/documentTitles';

class ServicesPage extends React.Component {
  render() {
    return (
      <DocumentTitle title={SERVICES}>
        <AppWrapper title="Services" />
      </DocumentTitle>
    );
  }
}

export default ServicesPage;
