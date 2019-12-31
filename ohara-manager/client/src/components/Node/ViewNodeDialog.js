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

import React, { useState, useCallback, useEffect } from 'react';
import { get, round, isEmpty, capitalize } from 'lodash';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import CheckCircleIcon from '@material-ui/icons/CheckCircle';
import Error from '@material-ui/icons/Error';
import CreateIcon from '@material-ui/icons/Create';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardContent from '@material-ui/core/CardContent';
import Table from '@material-ui/core/Table';
import TableHead from '@material-ui/core/TableHead';
import TableBody from '@material-ui/core/TableBody';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import Divider from '@material-ui/core/Divider';

import { FullScreenDialog, DeleteDialog } from 'components/common/Dialog';
import { Button } from 'components/common/Form';
import {
  useZookeeperState,
  useBrokerState,
  useWorkerState,
  useNodeState,
  useNodeActions,
  useViewNodeDialog,
  useEditNodeDialog,
} from 'context';
import EditNodeDialog from './EditNodeDialog';
import { state } from '../../api/nodeApi';
import { configuratorMode } from '../../api/inspectApi';
import * as streamApi from '../../api/streamApi';

const Wrapper = styled.div(
  ({ theme }) => css`
    .details {
      margin-top: ${theme.spacing(3)}px;
    }

    .MuiCardContent-root {
      padding: 0;
    }

    .MuiTableRow-root:nth-child(2n - 1) {
      background-color: ${theme.palette.grey[100]};
    }

    button {
      margin: ${props => props.theme.spacing(2)}px;
    }
  `,
);

const ServiceHead = styled(TableHead)`
  background-color: ${props => props.theme.palette.grey[100]};
`;

const ServiceCell = styled(TableCell)`
  background-color: ${props => props.theme.palette.common.white};
`;

const StateIcon = curState => {
  const curColor = props => {
    return curState === state.available
      ? props.theme.palette.success.main
      : props.theme.palette.error.main;
  };

  const StateDiv = styled.div`
    border-radius: 4px;
    border: 1px solid ${props => curColor(props)};
    color: ${props => curColor(props)};
    display: flex;
    align-items: center;
    width: 84px;
    justify-content: space-around;

    svg {
      width: 15px;
    }
  `;

  return (
    <StateDiv component="div">
      {curState === state.available ? <CheckCircleIcon /> : <Error />}
      <Typography>{curState === state.available ? 'Alive' : 'Dead'}</Typography>
    </StateDiv>
  );
};
StateIcon.propTypes = {
  theme: PropTypes.object,
};

const ViewNodeDialog = props => {
  const {
    isOpen,
    close: closeViewNodeDialog,
    data: nodeData,
  } = useViewNodeDialog();

  const { mode } = props;

  const { open: openEditNodeDialog } = useEditNodeDialog();
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const { isFetching: isDeleting } = useNodeState();
  const { deleteNode } = useNodeActions();

  const nodeName = get(nodeData, 'hostname', '');
  const { data: zookeepers } = useZookeeperState();
  const { data: brokers } = useBrokerState();
  const { data: workers } = useWorkerState();

  const [services, setServices] = useState([]);

  const fetchServices = useCallback(async () => {
    const services = get(nodeData, 'services', [])
      // we don't want to see configurator in our node service list
      .filter(service => service.name !== 'configurator')
      .map(service =>
        service.clusterKeys.map(clusterKey => {
          return {
            key: clusterKey,
            name: clusterKey.name,
            type: service.name,
            // workspace name will as same as the service name (zk, bk, wk)
            workspace: clusterKey.name,
          };
        }),
      )
      // since clusterKeys is array, we need to "flatten" object
      .flat(1)
      .sort((a, b) => (a.workspace < b.workspace ? -1 : 1));

    const result = await Promise.all(
      services.map(async service => {
        switch (service.type) {
          case 'zookeeper':
            delete service.key;
            return Object.assign(service, {
              status: get(
                zookeepers.find(zk => zk.name === service.name),
                'state',
                'Unknown',
              ),
            });
          case 'broker':
            delete service.key;
            return Object.assign(service, {
              status: get(
                brokers.find(bk => bk.name === service.name),
                'state',
                'Unknown',
              ),
            });
          case 'connect-worker':
            delete service.key;
            return Object.assign(service, {
              status: get(
                workers.find(wk => wk.name === service.name),
                'state',
                'Unknown',
              ),
            });
          case 'stream':
            const d = await streamApi.get(service.key).data;
            Object.assign(service, {
              status: d.state,
              workspace: d.brokerClusterKey.name,
            });
            delete service.key;
            return service;
          default:
            throw Error('Unknown service type');
        }
      }),
    );
    setServices(result);
  }, [zookeepers, brokers, workers, nodeData]);

  useEffect(() => {
    if (isEmpty(nodeData) || isEmpty(nodeData.services)) return;
    fetchServices();
  }, [nodeData, fetchServices]);

  const handleDelete = () => {
    deleteNode(nodeName);
    setIsConfirmOpen(false);
    closeViewNodeDialog();
  };

  const renderDataBody = () => {
    if (isEmpty(services)) return null;
    return services.map((service, rowIdx) => (
      <TableRow key={rowIdx}>
        {
          <>
            <ServiceCell key={rowIdx + '_name'}>{service.name}</ServiceCell>
            <ServiceCell key={rowIdx + '_type'}>{service.type}</ServiceCell>
            <ServiceCell key={rowIdx + '_workspace'}>
              {service.workspace}
            </ServiceCell>
            <ServiceCell key={rowIdx + '_status'}>
              {StateIcon(
                service.status === 'RUNNING'
                  ? state.available
                  : state.unavailable,
              )}
            </ServiceCell>
          </>
        }
      </TableRow>
    ));
  };

  return (
    <FullScreenDialog
      title="View node detail"
      open={isOpen}
      handleClose={closeViewNodeDialog}
      loading={isDeleting}
    >
      <Wrapper>
        <Grid container justify="space-between" alignItems="flex-end">
          <Grid item>
            <Typography component="h2" variant="overline" gutterBottom>
              NODES
            </Typography>
            <Typography component="h2" variant="h3">
              {nodeName}
            </Typography>
          </Grid>
          <Grid item>
            <Button
              variant="outlined"
              color="secondary"
              disabled={isEmpty(nodeData)}
              onClick={() => setIsConfirmOpen(true)}
            >
              Delete
            </Button>
            <DeleteDialog
              title="Delete node?"
              content={`Are you sure you want to delete the node: ${nodeName} ? This action cannot be undone!`}
              open={isConfirmOpen}
              handleClose={() => setIsConfirmOpen(false)}
              handleConfirm={handleDelete}
              isWorking={isDeleting}
            />
          </Grid>
        </Grid>
        <Grid container spacing={3} className="details">
          <Grid item xs={4}>
            <Card>
              <CardHeader title="Node Info" />
              <Divider />
              <CardContent>
                <Table>
                  <TableBody>
                    <TableRow>
                      <TableCell>Hostname</TableCell>
                      <TableCell>{nodeName}</TableCell>
                    </TableRow>
                    {mode !== configuratorMode.k8s && (
                      <>
                        <TableRow>
                          <TableCell>Port</TableCell>
                          <TableCell>
                            {get(nodeData, 'port', 'Unknown')}
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>User</TableCell>
                          <TableCell>
                            {get(nodeData, 'user', 'Unknown')}
                          </TableCell>
                        </TableRow>
                        <TableRow>
                          <TableCell>Password</TableCell>
                          <TableCell>
                            {get(nodeData, 'password', 'Unknown')}
                          </TableCell>
                        </TableRow>
                      </>
                    )}
                    {get(nodeData, 'resources', []).map(resource => (
                      <TableRow key={resource.name}>
                        <TableCell>{resource.name}</TableCell>
                        <TableCell>
                          {resource.used &&
                            `${round(resource.value * resource.used, 1)} ${
                              resource.unit
                            } / `}
                          {`${round(resource.value, 1)} ${resource.unit}`}
                        </TableCell>
                      </TableRow>
                    ))}
                    <TableRow>
                      <TableCell>State</TableCell>
                      <TableCell>
                        {StateIcon(get(nodeData, 'state', 'Unknown'))}
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
                {mode === configuratorMode.docker && (
                  <Button
                    variant="text"
                    startIcon={<CreateIcon />}
                    onClick={() => openEditNodeDialog(nodeData)}
                  >
                    Edit
                  </Button>
                )}
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={8}>
            <Card>
              <CardHeader title="Node Services" />
              <Divider />
              <CardContent>
                <Table size="small">
                  <ServiceHead>
                    <TableRow>
                      {!isEmpty(services) &&
                        Object.keys(services[0]).map(header => {
                          return (
                            <TableCell align="left" key={header}>
                              {capitalize(header)}
                            </TableCell>
                          );
                        })}
                    </TableRow>
                  </ServiceHead>
                  <TableBody>{renderDataBody()}</TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Wrapper>

      <EditNodeDialog />
    </FullScreenDialog>
  );
};

ViewNodeDialog.propTypes = {
  mode: PropTypes.string.isRequired,
};
export default ViewNodeDialog;
