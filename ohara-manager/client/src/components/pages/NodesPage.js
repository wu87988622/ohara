import React from 'react';

import DocumentTitle from 'react-document-title';

import { AppWrapper } from 'common/Layout';
import { NODES } from 'constants/documentTitles';

class NodesPage extends React.Component {
  render() {
    return (
      <DocumentTitle title={NODES}>
        <AppWrapper title="Nodes" />
      </DocumentTitle>
    );
  }
}

export default NodesPage;
