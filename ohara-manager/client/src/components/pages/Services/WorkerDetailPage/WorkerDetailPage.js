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

import List from './List';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { Item } from './Styles.js';

class WorkerDetailPage extends React.Component {
  static propTypes = {
    workers: PropTypes.arrayOf(
      PropTypes.shape({
        name: PropTypes.string.isRequired,
        clientPort: PropTypes.number.isRequired,
        nodeNames: PropTypes.arrayOf(PropTypes.string).isRequired,
        jarNames: PropTypes.arrayOf(PropTypes.string),
      }),
    ).isRequired,
    name: PropTypes.string.isRequired,
  };

  render() {
    const { workers, name } = this.props;
    const worker = workers.find(worker => worker.name === name);

    if (!worker) return null;

    const {
      name: workerName,
      clientPort,
      nodeNames,
      sources,
      sinks,
      jarNames,
    } = worker;

    // we just need the jar name not the full path
    const jarList = jarNames.map(name => name.split('/').pop());
    const pluginList = [...sources, ...sinks].map(({ className }) => className);

    return (
      <React.Fragment>
        <Box shadow={false}>
          <H2>Services > {workerName}</H2>
          <Item>
            <h5>Port:</h5>
            <span className="content">{clientPort}</span>
          </Item>
          <Item>
            <h5>Node List:</h5>
            <List list={nodeNames} />
          </Item>
          <Item>
            <h5>Jar List:</h5>
            <List list={jarList} />
          </Item>
          <Item>
            <h5>Plugin List:</h5>
            <List list={pluginList} />
          </Item>
        </Box>
      </React.Fragment>
    );
  }
}

export default WorkerDetailPage;
