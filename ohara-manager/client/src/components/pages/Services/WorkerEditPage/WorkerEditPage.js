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
import { find } from 'lodash';

import { Box } from 'common/Layout';
import { FormGroup, Label } from 'common/Form';
import { H2 } from 'common/Headings';

import * as s from './Styles';

class WorkerEditPage extends React.Component {
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
    const worker = find(workers, { name });
    if (!worker) return null;

    return (
      <React.Fragment>
        <s.TopWrapper>
          <H2>Connect Worker Service</H2>
        </s.TopWrapper>
        <Box shadow={false}>
          <FormGroup data-testid="name">
            <Label>Service name</Label>
            <s.Text width="14rem">{worker.name}</s.Text>
          </FormGroup>
          <FormGroup data-testid="client-port">
            <Label>Port</Label>
            <s.Text width="14rem">{worker.clientPort}</s.Text>
          </FormGroup>
          <s.FormRow>
            <s.FormCol width="16rem">
              <Label>Node List</Label>
              <s.List width="14rem">
                {worker.nodeNames &&
                  worker.nodeNames.map(nodeName => (
                    <s.ListItem key={nodeName}>{nodeName}</s.ListItem>
                  ))}
              </s.List>
            </s.FormCol>
            <s.FormCol width="20rem">
              <Label>Connector Plugin List</Label>
              <s.List width="18rem">
                {worker.jarNames &&
                  worker.jarNames.map(jarName => (
                    <s.ListItem key={jarName}>{jarName}</s.ListItem>
                  ))}
              </s.List>
            </s.FormCol>
          </s.FormRow>
        </Box>
      </React.Fragment>
    );
  }
}

export default WorkerEditPage;
