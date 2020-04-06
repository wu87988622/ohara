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
import { get, toNumber, size, filter, isEqual } from 'lodash';
import moment from 'moment';
import Grid from '@material-ui/core/Grid';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardActions from '@material-ui/core/CardActions';
import CardContent from '@material-ui/core/CardContent';
import Avatar from '@material-ui/core/Avatar';
import Button from '@material-ui/core/Button';
import InputIcon from '@material-ui/icons/Input';
import Typography from '@material-ui/core/Typography';

import { KIND } from 'const';
import * as context from 'context';
import * as hooks from 'hooks';
import { Dialog } from 'components/common/Dialog';
import { Tooltip } from 'components/common/Tooltip';
import { Wrapper } from './WorkspaceListStyles';

const Statistic = ({ value, label }) => (
  <>
    <Typography variant="h4" component="h2" align="center">
      {value}
    </Typography>
    <Typography
      variant="caption"
      component="h6"
      color="textSecondary"
      gutterBottom
      align="center"
    >
      {label}
    </Typography>
  </>
);

Statistic.propTypes = {
  value: PropTypes.number.isRequired,
  label: PropTypes.string.isRequired,
};

function WorkspaceList() {
  const switchWorkspace = hooks.useSwitchWorkspaceAction();
  const { isOpen, close } = context.useListWorkspacesDialog();
  const workspaces = hooks.useAllWorkspaces();
  const workspaceName = hooks.useWorkspaceName();
  const allTopics = hooks.useAllTopics();
  const isTopicLoaded = hooks.useIsTopicLoaded();
  const fetchAllTopics = hooks.useFetchAllTopicsAction();

  // we need to fetch all topic across all workspaces directly here
  React.useEffect(() => {
    if (!isTopicLoaded) fetchAllTopics();
  }, [fetchAllTopics, isTopicLoaded]);

  const handleClick = name => () => {
    switchWorkspace(name);
    close();
  };

  const pickBrokerKey = object => {
    return {
      name: get(object, 'brokerClusterKey.name'),
      group: get(object, 'brokerClusterKey.group'),
    };
  };

  return (
    <>
      <Dialog
        open={isOpen}
        handleClose={close}
        title={`Showing ${size(workspaces)} workspaces`}
        showActions={false}
        maxWidth="md"
      >
        <Wrapper>
          <Grid container spacing={2}>
            {workspaces.map(workspace => {
              const name = get(workspace, 'name');
              const nodeNames = get(workspace, 'nodeNames');

              const lastModified = get(
                workspace,
                // TODO: Displaying `stagingSettings` for now. This is not the correct
                // place to get the date, we should come back and fix this
                'stagingSettings.lastModified',
              );

              const avatarText = name.substring(0, 2).toUpperCase();
              const updatedText = moment(toNumber(lastModified)).fromNow();

              const isActive = name === workspaceName;
              const brokerKey = { name, group: KIND.broker };
              const count = {
                nodes: size(nodeNames),
                pipelines: 0, // TODO: See the issue (https://github.com/oharastream/ohara/issues/3506)
                topics: size(
                  filter(allTopics, topic =>
                    isEqual(pickBrokerKey(topic), brokerKey),
                  ),
                ),
              };

              return (
                <Grid item xs={4} key={name}>
                  <Card className={isActive ? 'active-workspace' : ''}>
                    <CardHeader
                      avatar={
                        <Avatar className="workspace-icon">{avatarText}</Avatar>
                      }
                      title={name}
                      subheader={`Updated: ${updatedText}`}
                    />
                    <CardContent>
                      <Grid container spacing={2}>
                        <Grid item xs={6}>
                          <Statistic value={count.nodes} label="Nodes" />
                        </Grid>

                        {/* Feature is disabled because it's not implemented in 0.9 */
                        false && (
                          <Grid item xs={4}>
                            <Statistic
                              value={count.pipelines}
                              label="Pipelines"
                            />
                          </Grid>
                        )}
                        <Grid item xs={6}>
                          <Statistic value={count.topics} label="Topics" />
                        </Grid>
                      </Grid>
                    </CardContent>
                    <CardActions>
                      <Tooltip
                        title={
                          isActive
                            ? "This is the workspace that you're at now"
                            : ''
                        }
                        placement="top"
                        enterDelay={isActive ? 250 : 1000}
                        className="call-to-action"
                      >
                        <Button
                          size="large"
                          startIcon={<InputIcon />}
                          onClick={handleClick(name)}
                          disabled={isActive}
                        >
                          Into Workspace
                        </Button>
                      </Tooltip>
                    </CardActions>
                  </Card>
                </Grid>
              );
            })}
          </Grid>
        </Wrapper>
      </Dialog>
    </>
  );
}

export default WorkspaceList;
