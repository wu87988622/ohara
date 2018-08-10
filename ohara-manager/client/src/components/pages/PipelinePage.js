import React from 'react';
import DocumentTitle from 'react-document-title';

import AppWrapper from '../common/AppWrapper';
import { PIPELINE } from '../../constants/documentTitles';

class PipelinePage extends React.Component {
  render() {
    return (
      <DocumentTitle title={PIPELINE}>
        <AppWrapper title="Pipeline" />
      </DocumentTitle>
    );
  }
}

export default PipelinePage;
