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

import { StyledH4, NodeNamesList } from './styles';

const NodeNames = props => {
  const { connectorName, nodeNames } = props;

  return (
    <>
      <StyledH4>
        <i className="fas fa-sitemap"></i>
        {`Node (${connectorName})`}
      </StyledH4>

      <NodeNamesList>
        {nodeNames.map(nodeName => (
          <li key={nodeName} className="item" data-testid="detail-item">
            <div className="item-header">Name</div>
            <div className="item-body">
              <span className="item-value">{nodeName} </span>
            </div>
          </li>
        ))}
      </NodeNamesList>
    </>
  );
};

NodeNames.propTypes = {
  connectorName: PropTypes.string.isRequired,
  nodeNames: PropTypes.array.isRequired,
};

export default NodeNames;
