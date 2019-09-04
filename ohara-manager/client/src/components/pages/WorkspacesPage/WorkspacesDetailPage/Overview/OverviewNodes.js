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

import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import TableRow from '@material-ui/core/TableRow';
import Tooltip from '@material-ui/core/Tooltip';
import { capitalize } from 'lodash';

import OverviewTable from './OverviewTable';
import {
  TabHeading,
  StyledIcon,
  StyledIconLink,
  StyledTableCell,
  TooltipBody,
} from './styles';

const OverviewNodes = props => {
  const { worker, handleRedirect, broker, zookeeper } = props;

  const [nodes, setNodes] = useState([]);

  useEffect(() => {
    const { nodeNames, clientPort, jmxPort } = worker;
    const workerNodes = nodeNames.map((nodeName, idx) => {
      return {
        clusterType: 'Worker',
        nodeName: `${nodeName}:${clientPort}`,
        moreInfo: { jmxPort },
        shouldRender: idx === 0, // Only renders the first cluster type
      };
    });

    setNodes(prevNodes => [...prevNodes, ...workerNodes]);
  }, [worker]);

  useEffect(() => {
    const fetchBroker = async () => {
      if (!broker) return;

      const { nodeNames, clientPort, exporterPort, jmxPort } = broker;
      const brokerNodes = nodeNames.map((nodeName, idx) => {
        return {
          clusterType: 'Broker',
          nodeName: `${nodeName}:${clientPort}`,
          moreInfo: { exporterPort, jmxPort },
          shouldRender: idx === 0,
        };
      });

      setNodes(prevNodes => [...prevNodes, ...brokerNodes]);
    };

    fetchBroker();
  }, [broker]);

  // Since zookeeper is the last request we sent, we just need to wait for this
  // in longer term, we should have a better way to determine if a request a
  // finish or not individually
  const [isFetchingZookeeper, setIsFetchingZookeeper] = useState(true);

  useEffect(() => {
    setIsFetchingZookeeper(false);
    if (!zookeeper || Array.isArray(zookeeper)) return;
    const { nodeNames, clientPort, electionPort, peerPort } = zookeeper;
    const zookeeperNodes = nodeNames.map((nodeName, idx) => {
      return {
        clusterType: 'Zookeeper',
        nodeName: `${nodeName}:${clientPort}`,
        moreInfo: { electionPort, peerPort },
        shouldRender: idx === 0,
      };
    });

    setNodes(prevNodes => [...prevNodes, ...zookeeperNodes]);
  }, [zookeeper]);

  return (
    <>
      <TabHeading>
        <StyledIcon marginright="15" className="fas fa-sitemap" />
        <span className="title">Nodes</span>
        <StyledIconLink onClick={() => handleRedirect('nodes')}>
          <Tooltip title="Link to Nodes page" enterDelay={1000}>
            <StyledIcon
              className="fas fa-external-link-square-alt"
              data-testid="overview-nodes-link"
            />
          </Tooltip>
        </StyledIconLink>
      </TabHeading>

      <OverviewTable
        headers={['Cluster type', 'Node', 'More info']}
        isLoading={isFetchingZookeeper}
      >
        {// Use idx as the key as we don't have a more reliable value
        // to use as the key
        nodes.map((node, idx) => {
          const { clusterType, nodeName, shouldRender, moreInfo } = node;
          return (
            <TableRow key={idx}>
              <StyledTableCell component="td" data-testid={'node-type'}>
                {shouldRender && clusterType}
              </StyledTableCell>
              <StyledTableCell component="td">{nodeName}</StyledTableCell>
              <StyledTableCell component="td" align="right">
                <Tooltip
                  interactive
                  placement="right"
                  data-testid={`${clusterType}-${nodeName}`}
                  title={
                    <TooltipBody data-testid={`${clusterType}-${nodeName}-tip`}>
                      {moreInfo &&
                        Object.keys(moreInfo).map(key => (
                          <li key={key}>
                            {capitalize(key)}: {moreInfo[key]}
                          </li>
                        ))}
                    </TooltipBody>
                  }
                >
                  <StyledIcon className="fas fa-info-circle" size="13" />
                </Tooltip>
              </StyledTableCell>
            </TableRow>
          );
        })}
      </OverviewTable>
    </>
  );
};

OverviewNodes.propTypes = {
  handleRedirect: PropTypes.func.isRequired,
  worker: PropTypes.shape({
    nodeNames: PropTypes.arrayOf(PropTypes.string).isRequired,
    clientPort: PropTypes.number.isRequired,
    jmxPort: PropTypes.number.isRequired,
  }),
  broker: PropTypes.object,
  zookeeper: PropTypes.object,
};

export default OverviewNodes;
