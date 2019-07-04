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
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';

import { SortTable } from 'components/common/Mui/Table';
import { workersPropType } from 'propTypes/services';
import { StyledIcon } from './styles';

const WorkspacesListPage = props => {
  const { workers, isLoading } = props;

  const headRows = [
    { id: 'name', label: 'Name' },
    { id: 'nodes', label: 'Nodes' },
    { id: 'action', label: 'Action', sortable: false },
  ];

  const actionButton = data => {
    const { name } = data;
    return (
      <Tooltip title={`Link to ${name} page`} enterDelay={1000}>
        <IconButton data-testid={name} onClick={() => handleRedirect(name)}>
          <StyledIcon className="fas fa-external-link-square-alt" />
        </IconButton>
      </Tooltip>
    );
  };

  const rows = workers.map(d => {
    return {
      name: d.name,
      nodes: d.nodeNames.join(','),
      action: actionButton(d),
    };
  });

  const handleRedirect = workspaceName => {
    props.history.push(`/workspaces/${workspaceName}/overview`);
  };

  return <SortTable isLoading={isLoading} headRows={headRows} rows={rows} />;
};

WorkspacesListPage.propTypes = {
  workers: PropTypes.arrayOf(workersPropType).isRequired,
  isLoading: PropTypes.bool,
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
};

export default WorkspacesListPage;
