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
import { isEmpty, map, get } from 'lodash';

import * as zookeeperApi from 'api/zookeeperApi';
import { Box } from 'components/common/Layout';
import { H2 } from 'components/common/Headings';
import { TableLoader } from 'components/common/Loader';
import { FormGroup } from 'components/common/Form';

import * as s from './styles';

class ZookeeperListPage extends React.Component {
  state = {
    isLoading: true,
    zookeeper: [],
  };

  zookeeperHeaders = ['SERVICES', 'PORT', 'NODES'];

  componentDidMount() {
    this.fetchZookeepers();
  }

  fetchZookeepers = async () => {
    const res = await zookeeperApi.fetchZookeepers();
    const zookeepers = get(res, 'data.result', []);
    if (!isEmpty(zookeepers)) {
      this.setState({ zookeeper: zookeepers[0] });
    }
    this.setState(() => ({ isLoading: false }));
  };

  getNodeList = () => {
    const { zookeeper } = this.state;
    if (!zookeeper) return [];
    return map(zookeeper.nodeNames, nodeName => {
      return `${nodeName}:${zookeeper.clientPort}`;
    });
  };

  render() {
    const { isLoading, zookeeper } = this.state;
    return (
      <React.Fragment>
        <Box>
          <FormGroup isInline>
            <H2>Services > Zookeeper</H2>
          </FormGroup>
          {isLoading ? (
            <TableLoader />
          ) : (
            <s.Table headers={this.zookeeperHeaders}>
              {isEmpty(zookeeper) ? (
                <tr />
              ) : (
                <tr>
                  <td>{zookeeper.name}</td>
                  <td>{zookeeper.clientPort}</td>
                  <td>
                    {zookeeper.nodeNames.map(nodeName => (
                      <div key={nodeName}>{nodeName}</div>
                    ))}
                  </td>
                </tr>
              )}
            </s.Table>
          )}
        </Box>
      </React.Fragment>
    );
  }
}

export default ZookeeperListPage;
