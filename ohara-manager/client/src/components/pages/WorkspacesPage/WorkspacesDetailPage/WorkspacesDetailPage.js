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
import PropTypes from 'prop-types';
import DocumentTitle from 'react-document-title';
import { get } from 'lodash';

import Container from 'components/common/Mui/Layout';
import Tabs from './Tabs';
import { WORKSPACES_DETAIL } from 'constants/documentTitles';
import { PageTitle } from 'components/common/Mui/Typography';
import { StyledIcon } from './styles';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

const WorkspacesDetailPage = props => {
  const { workspaceName } = props.match.params;
  const { data: response } = useApi.useFetchApi(URL.WORKER_URL, workspaceName);
  const worker = get(response, 'data.result', null);

  if (!worker) return null;

  return (
    <DocumentTitle title={WORKSPACES_DETAIL}>
      <Container>
        <PageTitle>
          Workspaces <StyledIcon className="fas fa-chevron-right" />
          {workspaceName}
        </PageTitle>

        <Tabs {...props} worker={worker} />
      </Container>
    </DocumentTitle>
  );
};

WorkspacesDetailPage.propTypes = {
  match: PropTypes.shape({
    params: PropTypes.shape({
      workspaceName: PropTypes.string.isRequired,
    }).isRequired,
  }).isRequired,
};

export default WorkspacesDetailPage;
