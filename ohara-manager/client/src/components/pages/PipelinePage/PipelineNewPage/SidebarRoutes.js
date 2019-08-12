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

import * as Connectors from '../Connectors';
import * as PIPELINES from 'constants/pipelines';

const SidebarRoutes = props => {
  const { connectorProps, connectors } = props;
  const routeBaseUrl = `/pipelines/(new|edit)`;

  const getConnectorDefs = ({ connectors, type }) => {
    const getByClassName = connector => connector.className === type;
    const connector = connectors.find(getByClassName);
    return connector.definitions;
  };

  const {
    jdbcSource,
    ftpSource,
    perfSource,
    hdfsSink,
    ftpSink,
    customSource,
    customSink,
  } = PIPELINES.CONNECTOR_TYPES;

  return (
    <>
      <Route
        path={`${routeBaseUrl}/${jdbcSource}`}
        render={() => (
          <Connectors.JdbcSource
            {...connectorProps}
            defs={getConnectorDefs({
              connectors,
              type: jdbcSource,
            })}
          />
        )}
      />

      <Route
        path={`${routeBaseUrl}/${ftpSource}`}
        render={() => (
          <Connectors.FtpSource
            {...connectorProps}
            defs={getConnectorDefs({
              connectors,
              type: ftpSource,
            })}
          />
        )}
      />

      <Route
        path={`${routeBaseUrl}/${perfSource}`}
        render={() => (
          <Connectors.PerfSource
            {...connectorProps}
            defs={getConnectorDefs({
              connectors,
              type: perfSource,
            })}
          />
        )}
      />

      <Route
        path={`${routeBaseUrl}/${ftpSink}`}
        render={() => (
          <Connectors.FtpSink
            {...connectorProps}
            defs={getConnectorDefs({
              connectors,
              type: ftpSink,
            })}
          />
        )}
      />

      <Route
        path={`${routeBaseUrl}/topic`}
        render={() => <Connectors.Topic {...connectorProps} />}
      />

      <Route
        path={`${routeBaseUrl}/${hdfsSink}`}
        render={() => (
          <Connectors.HdfsSink
            {...connectorProps}
            defs={getConnectorDefs({
              connectors,
              type: hdfsSink,
            })}
          />
        )}
      />

      <Route
        path={`${routeBaseUrl}/stream`}
        render={() => <Connectors.StreamApp {...connectorProps} />}
      />

      <Route
        path={`${routeBaseUrl}/${customSource}`}
        render={() => (
          <Connectors.CustomConnector
            {...connectorProps}
            defs={getConnectorDefs({
              connectors,
              type: customSource,
            })}
          />
        )}
      />

      <Route
        path={`${routeBaseUrl}/${customSink}`}
        render={() => (
          <Connectors.CustomConnector
            {...connectorProps}
            defs={getConnectorDefs({
              connectors,
              type: customSink,
            })}
          />
        )}
      />
    </>
  );
};

SidebarRoutes.propTypes = {
  connectorProps: PropTypes.object.isRequired,
  connectors: PropTypes.array.isRequired,
};

export default SidebarRoutes;
