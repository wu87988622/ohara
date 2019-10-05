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
import { isEmpty } from 'lodash';

import NodeNameList from './NodeNameList';
import { graph as graphPropType } from 'propTypes/pipeline';
import { Box } from './styles';
import * as utils from '../../Connectors/connectorUtils';

const NodeNames = props => {
  // If it's an empty canvas don't render anything
  if (isEmpty(props.graph)) return null;

  const { graph, match } = props;

  const { connectorName } = match.params;
  const currentConnector = graph.find(g => g.name === connectorName);

  if (
    !currentConnector ||
    (currentConnector.className.split('.').pop() !== 'JsonIn' &&
      currentConnector.className.split('.').pop() !== 'JsonOut')
  ) {
    return null;
  }
  let tasks = [];
  if (currentConnector.state === 'RUNNING') {
    [, , , , tasks] = utils.useFetchConnectors(props);
  }

  const nodeNames = tasks.map(task => task.nodeName);
  if (nodeNames.length === 0) return null;
  return (
    <Box>
      <NodeNameList connectorName={connectorName} nodeNames={nodeNames} />
    </Box>
  );
};

NodeNames.propTypes = {
  graph: PropTypes.arrayOf(graphPropType).isRequired,
  match: PropTypes.shape({
    params: PropTypes.shape({
      connectorName: PropTypes.string,
    }).isRequired,
  }).isRequired,
  nodeNames: PropTypes.array.isRequired,
};

export default NodeNames;
