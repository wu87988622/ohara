import React from 'react';

import DocumentTitle from 'react-document-title';

import { AppWrapper } from 'common/Layout';
import { MONITORING } from 'constants/documentTitles';

class MonitoringPage extends React.Component {
  render() {
    return (
      <DocumentTitle title={MONITORING}>
        <AppWrapper title="Monitoring" />
      </DocumentTitle>
    );
  }
}

export default MonitoringPage;
