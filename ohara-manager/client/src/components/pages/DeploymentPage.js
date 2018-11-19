import React from 'react';

import DocumentTitle from 'react-document-title';

import { AppWrapper } from 'common/Layout';
import { DEPLOYMENT } from 'constants/documentTitles';

class DeploymentPage extends React.Component {
  render() {
    return (
      <DocumentTitle title={DEPLOYMENT}>
        <AppWrapper title="Deployment" />
      </DocumentTitle>
    );
  }
}

export default DeploymentPage;
