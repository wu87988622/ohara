/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import DocumentTitle from 'react-document-title';

import { AppWrapper } from 'common/Layout';
import { NOT_FOUND_PAGE } from 'constants/documentTitles';

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
