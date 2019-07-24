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

import TopicMetrics from './TopicMetrics';
import ConnectorMetrics from './ConnectorMetrics';
import { graph as graphPropType } from 'propTypes/pipeline';
import { Box } from './styles';

const Metrics = props => {
  // If it's an empty canvas don't render anything
  if (isEmpty(props.graph)) return null;

  const { graph, match } = props;

  // TODO: add an active state here so we won't need to reach
  // for connectorName from the router
  const { connectorName } = match.params;
  const currentConnector = graph.find(g => g.name === connectorName);

  // When a new connector is added the first on the canvas, there's
  // no current/active connector at the time, return at this point
  // so we won't break the app
  if (!currentConnector) return null;

  const {
    metrics: { meters },
    kind,
  } = currentConnector;

  if (isEmpty(meters)) return null;

  const meterData = meters.map(meter => meter);

  return (
    <Box>
      {kind === 'topic' ? (
        <TopicMetrics topicName={connectorName} meters={meterData} />
      ) : (
        <ConnectorMetrics connectorName={connectorName} meters={meterData} />
      )}
    </Box>
  );
};

Metrics.propTypes = {
  graph: PropTypes.arrayOf(graphPropType).isRequired,
  match: PropTypes.shape({
    params: PropTypes.shape({
      connectorName: PropTypes.string,
    }).isRequired,
  }).isRequired,
};

export default Metrics;
