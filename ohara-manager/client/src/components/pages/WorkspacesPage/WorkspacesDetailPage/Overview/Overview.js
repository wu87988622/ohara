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
import { get, isNull } from 'lodash';

import OverviewTopics from './OverviewTopics';
import OverviewConnectors from './OverviewConnectors';
import OverviewStreamApps from './OverviewStreamApps';
import OverviewPlugin from './OverviewPlugin';
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
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

const Overview = props => {
  const { worker } = props;
  const { connectors } = worker;

  const {
    imageName: workerImageName,
    name: workerName,
    tags,
  } = worker.settings;

  const {
    name: zookeeperClusterName,
    imageName: zookeeperImageName,
  } = tags.zookeeper;

  const { name: brokerClusterName, imageName: borkerImageName } = tags.broker;

  const { data: brokerRes } = useApi.useFetchApi(
    `${URL.BROKER_URL}/${brokerClusterName}`,
  );
  const broker = get(brokerRes, 'data.result', null);

  const { data: zookeeperRes } = useApi.useFetchApi(
    `${URL.ZOOKEEPER_URL}/${zookeeperClusterName}`,
  );
  const zookeeper = get(zookeeperRes, 'data.result', null);

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
            <li>Worker Image: {workerImageName}</li>
            <li>Broker Image: {borkerImageName}</li>
            <li>Zookeeper Image: {zookeeperImageName}</li>
          </List>
        </Box>

        <Box>
          {!isNull(zookeeper) && !isNull(broker) && (
            <OverviewNodes
              worker={worker}
              handleRedirect={handleRedirect}
              zookeeper={zookeeper}
              broker={broker}
            />
          )}
        </Box>

        <Box>
          <OverviewTopics
            handleRedirect={handleRedirect}
            workerName={workerName}
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

        <Box>
          <OverviewPlugin
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
    settings: PropTypes.shape({
      imageName: PropTypes.string.isRequired,
      brokerClusterName: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
      tags: PropTypes.object,
    }),
    connectors: PropTypes.array.isRequired,
  }).isRequired,
};

export default Overview;
