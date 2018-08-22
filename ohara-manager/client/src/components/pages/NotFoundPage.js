import React from 'react';
import DocumentTitle from 'react-document-title';

import { AppWrapper } from '../common/Layout';
import { NOT_FOUND_PAGE } from '../../constants/documentTitles';

class NotFoundPage extends React.Component {
  render() {
    return (
      <DocumentTitle title={NOT_FOUND_PAGE}>
        <AppWrapper title="Oops, page not found" />
      </DocumentTitle>
    );
  }
}

export default NotFoundPage;
