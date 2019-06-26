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

import OverviewTopics from './OverviewTopics';
import OverviewConnectors from './OverviewConnectors';
import OverviewStreamApps from './OverviewStreamApps';
import OverviewNodes from './OverviewNodes';
import {
  TabHeading,
  List,
  StyledIcon,
  Wrapper,
  LeftColumn,
  RightColumn,
  Box,
} from './styles';

const Overview = props => {
  const { worker } = props;
  const { imageName, brokerClusterName, name: workerName, connectors } = worker;

  const handleRedirect = service => {
    props.history.push(service);
  };

  return (
    <Wrapper>
      <LeftColumn>
        <Box>
          <TabHeading>
            <StyledIcon className="fas fa-info-circle" />
            <span className="title">Basic info</span>
          </TabHeading>
          <List>
            <li>Image: {imageName}</li>
          </List>
        </Box>

        <Box>
          <OverviewNodes worker={worker} handleRedirect={handleRedirect} />
        </Box>

        <Box>
          <OverviewTopics
            handleRedirect={handleRedirect}
            brokerClusterName={brokerClusterName}
          />
        </Box>
      </LeftColumn>

      <RightColumn>
        <Box>
          <OverviewConnectors connectors={connectors} />
        </Box>

        <Box>
          <OverviewStreamApps
            workerName={workerName}
            handleRedirect={handleRedirect}
          />
        </Box>
      </RightColumn>
    </Wrapper>
  );
};

Overview.propTypes = {
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
  worker: PropTypes.shape({
    imageName: PropTypes.string.isRequired,
    brokerClusterName: PropTypes.string.isRequired,
    workerName: PropTypes.string.isRequired,
    connectors: PropTypes.array.isRequired,
  }).isRequired,
};

export default Overview;
