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
import Paper from '@material-ui/core/Paper';
import styled from 'styled-components';
import { get, isEmpty } from 'lodash';

import { H4 } from 'components/common/Mui/Typography';
import { graph as graphPropType } from 'propTypes/pipeline';

const Box = styled(Paper)`
  margin-bottom: 20px;
  padding: 25px;
`;

const StyledH4 = styled(H4)`
  margin-top: 0;
`;

const Metrics = styled.ul`
  .item {
    color: ${props => props.theme.palette.text.secondary};
  }

  .item-header {
    font-size: 11px;
    margin-bottom: ${props => props.theme.spacing(1)}px;
  }

  .item-body {
    font-size: 14px;
    display: flex;
    margin-bottom: ${props => props.theme.spacing(2)}px;
    border-bottom: 1px solid ${props => props.theme.palette.text.secondary};
  }

  .item-value {
    margin-bottom: ${props => props.theme.spacing(1)}px;
  }

  .item-unit {
    margin-left: auto;
  }
`;

const MetricsSidebar = props => {
  // If it's an empty canvas
  if (isEmpty(props.graph)) return null;

  const { graph, match } = props;

  // TODO: add an active state here so we won't need to reach
  // for connectorName from the router
  const { connectorName } = match.params;
  const currentConnector = graph.find(g => g.name === connectorName);

  if (!currentConnector) return null;

  const { metrics } = currentConnector;
  const numberOfRows = get(metrics, 'meters[0].value', 0);
  const sizeOfRows = get(metrics, 'meters[1].value', 0);

  return (
    <Box>
      <StyledH4>
        <i className="fas fa-tachometer-alt"></i> {`Metrics (${connectorName})`}
      </StyledH4>

      <Metrics>
        <li className="item">
          <div className="item-header">Size (in bytes) of rows</div>
          <div className="item-body">
            <span className="item-value">{sizeOfRows}</span>
            <span className="item-unit">bytes</span>
          </div>
        </li>
        <li className="item">
          <div className="item-header">Number of rows</div>
          <div className="item-body">
            <span className="item-value">{numberOfRows} </span>
            <span className="item-unit">bytes</span>
          </div>
        </li>
      </Metrics>
    </Box>
  );
};

MetricsSidebar.propTypes = {
  graph: PropTypes.arrayOf(graphPropType).isRequired,
  match: PropTypes.shape({
    params: PropTypes.shape({
      connectorName: PropTypes.string,
    }).isRequired,
  }).isRequired,
};

export default MetricsSidebar;
