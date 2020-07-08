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
import { merge, flatMap, find, map, sortBy } from 'lodash';

import { GROUP, ServiceName } from 'const';
import Table from 'components/common/Table/MuiTable';
import ClusterStateChip from './ClusterStateChip';

function NodeServiceTable({ node }) {
  if (!node) return null;

  const flatClusters = flatMap(node?.services, (service) => {
    const { name, clusterKeys, clusters } = service;
    // we don't want to see configurator in our node service list
    if (name === ServiceName.CONFIGURATOR) return [];
    return map(clusterKeys, (clusterKey) => {
      const cluster = find(
        clusters,
        (cluster) => cluster.name === clusterKey.name,
      );
      return merge(clusterKey, {
        serviceName: name,
        workspaceName:
          clusterKey.group === GROUP.DEFAULT ? GROUP.DEFAULT : clusterKey.name,
        state: cluster?.state,
      });
    });
  });

  return (
    <Table
      columns={[
        { title: 'Name', field: 'name' },
        {
          title: 'Type',
          field: 'serviceName',
        },
        {
          title: 'Workspace',
          field: 'workspaceName',
        },
        {
          title: 'State',
          field: 'state',
          render: (cluster) => <ClusterStateChip cluster={cluster} />,
        },
      ]}
      data={sortBy(flatClusters, 'workspaceName')}
      options={{
        paging: false,
        rowStyle: null,
        search: flatClusters.length > 5,
        selection: false,
      }}
      title="Services"
    />
  );
}

NodeServiceTable.propTypes = {
  node: PropTypes.shape({
    services: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string,
        clusterKeys: PropTypes.arrayOf(
          PropTypes.shape({
            group: PropTypes.string,
            name: PropTypes.string,
          }),
        ),
        clusters: PropTypes.arrayOf(
          PropTypes.shape({
            group: PropTypes.string,
            name: PropTypes.string,
            state: PropTypes.string,
          }),
        ),
      }),
    ),
  }),
};

export default NodeServiceTable;
