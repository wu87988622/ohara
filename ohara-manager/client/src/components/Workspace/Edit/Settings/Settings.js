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

import React, { useEffect, useRef } from 'react';
import { get, sortBy, noop } from 'lodash';
import styled, { css } from 'styled-components';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import { Form } from 'react-final-form';

import { useWorkspace } from 'context';
import { useEditWorkspaceDialog } from 'context';
import { Segments } from 'components/Workspace/Edit';
import { QuickSearch } from 'components/common/Search';
import { RenderDefinition } from 'components/common/Definitions';

export const Wrapper = styled.div(
  ({ theme }) => css`
    .segments {
      margin-top: ${theme.spacing(2)}px;
      height: calc(100vh - 337px - 48px - 16px);
      overflow-y: auto;
    }
  `,
);

const sortByOrder = (definitions = []) => sortBy(definitions, 'orderInGroup');

const Settings = () => {
  const { currentWorker, currentBroker, currentZookeeper } = useWorkspace();
  const { data = {} } = useEditWorkspaceDialog();
  const { segment = Segments.WORKER } = data;

  const wkEl = useRef(null);
  const bkEl = useRef(null);
  const zkEl = useRef(null);

  useEffect(() => {
    const scrollOptions = {
      block: 'start',
      behavior: 'smooth',
    };
    switch (segment) {
      case Segments.WORKER:
        wkEl.current.scrollIntoView(scrollOptions);
        break;
      case Segments.BROKER:
        bkEl.current.scrollIntoView(scrollOptions);
        break;
      case Segments.ZOOKEEPER:
        zkEl.current.scrollIntoView(scrollOptions);
        break;
      default:
        break;
    }
  }, [segment]);

  const workerDefinitions = sortByOrder(
    get(currentWorker, 'settingDefinitions'),
  );

  const brokerDefinitions = sortByOrder(
    get(currentBroker, 'settingDefinitions'),
  );

  const zookeeperDefinitions = sortByOrder(
    get(currentZookeeper, 'settingDefinitions'),
  );

  return (
    <Wrapper>
      <Grid container justify="space-between" alignItems="center">
        <Typography variant="h4" gutterBottom>
          Settings
        </Typography>
        <QuickSearch data={workerDefinitions} />
      </Grid>
      <Grid container className="segments">
        <Grid item ref={wkEl}>
          <Typography variant="h4" gutterBottom>
            Worker
          </Typography>
          <Form
            onSubmit={noop}
            render={({ handleSubmit }) => {
              return (
                <form onSubmit={handleSubmit}>
                  {workerDefinitions.map(definition =>
                    RenderDefinition({
                      def: definition,
                      fieldProps: {
                        id: `worker-${definition.key}`,
                        margin: 'normal',
                      },
                    }),
                  )}
                </form>
              );
            }}
          />
        </Grid>
        <Grid item ref={bkEl}>
          <Typography variant="h4" gutterBottom>
            Broker
          </Typography>
          <Form
            onSubmit={noop}
            render={({ handleSubmit }) => {
              return (
                <form onSubmit={handleSubmit}>
                  {brokerDefinitions.map(definition =>
                    RenderDefinition({
                      def: definition,
                      fieldProps: {
                        id: `broker-${definition.key}`,
                        margin: 'normal',
                      },
                    }),
                  )}
                </form>
              );
            }}
          />
        </Grid>
        <Grid item ref={zkEl}>
          <Typography variant="h4" gutterBottom>
            Zookeeper
          </Typography>
          <Form
            onSubmit={noop}
            render={({ handleSubmit }) => {
              return (
                <form onSubmit={handleSubmit}>
                  {zookeeperDefinitions.map(definition =>
                    RenderDefinition({
                      def: definition,
                      fieldProps: {
                        id: `zookeeper-${definition.key}`,
                        margin: 'normal',
                      },
                    }),
                  )}
                </form>
              );
            }}
          />
        </Grid>
      </Grid>
    </Wrapper>
  );
};

export default Settings;
