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

import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import DocumentTitle from 'react-document-title';
import { isEmpty, get } from 'lodash';

import * as workerApi from 'api/workerApi';
import Container from 'components/common/Mui/Layout';
import WorkspacesNewModal from './WorkspacesNewModal';
import WorkspacesListPage from './WorkspacesListPage';
import { WORKSPACES } from 'constants/documentTitles';
import { PageTitle } from 'components/common/Mui/Typography';
import { PageHeader, StyledButton } from './styles';

const WorkspacesPage = props => {
  const [workers, setWorkers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const fetchData = async () => {
    const res = await workerApi.fetchWorkers();
    const workers = get(res, 'data.result', []);
    setIsLoading(false);

    if (!isEmpty(workers)) {
      setWorkers(workers);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  return (
    <DocumentTitle title={WORKSPACES}>
      <Container>
        <WorkspacesNewModal
          isActive={isModalOpen}
          onConfirm={fetchData}
          onClose={() => setIsModalOpen(false)}
        />

        <PageHeader>
          <PageTitle>Workspaces</PageTitle>

          <StyledButton
            onClick={() => setIsModalOpen(true)}
            text="New workspace"
          >
            New workspace
          </StyledButton>
        </PageHeader>

        <WorkspacesListPage
          {...props}
          workers={workers}
          isLoading={isLoading}
        />
      </Container>
    </DocumentTitle>
  );
};

WorkspacesPage.propTypes = {
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
};

export default WorkspacesPage;
