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
import { Route } from 'react-router-dom';

import { Connector, Topic, StreamApp } from '../Connectors';

const SidebarRoutes = props => {
  const { connectorProps } = props;
  const { match } = connectorProps;
  const { workspaceName, pipelineName } = match.params;

  const routeBaseUrl = `/pipelines/edit/${workspaceName}/${pipelineName}`;

  return (
    <div data-testid="pipeline-config-form">
      <Route
        path={`${routeBaseUrl}/(source|sink)`}
        render={() => <Connector {...connectorProps} />}
      />
      <Route
        path={`${routeBaseUrl}/topic`}
        render={() => <Topic {...connectorProps} />}
      />
      <Route
        path={`${routeBaseUrl}/stream`}
        render={() => <StreamApp {...connectorProps} />}
      />
    </div>
  );
};

SidebarRoutes.propTypes = {
  connectorProps: PropTypes.shape({
    match: PropTypes.shape({
      params: PropTypes.shape({
        workspaceName: PropTypes.string.isRequired,
        pipelineName: PropTypes.string.isRequired,
      }).isRequired,
    }).isRequired,
  }).isRequired,
};

export default SidebarRoutes;
