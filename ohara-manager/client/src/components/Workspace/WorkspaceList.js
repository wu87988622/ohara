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
import cx from 'classnames';
import { toNumber, size, filter, isEqual } from 'lodash';
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
import WarningIcon from '@material-ui/icons/Warning';

import { KIND } from 'const';
import * as hooks from 'hooks';
import { Dialog } from 'components/common/Dialog';
import { IconWrapper } from 'components/common/Icon';
import { Tooltip } from 'components/common/Tooltip';
import { Wrapper } from './WorkspaceListStyles';
import { isStable as isStableWorkspace } from 'utils/workspace';

const Statistic = ({ value, label }) => (
  <>
    <Typography align="center" component="h2" variant="h4">
      {value}
    </Typography>
    <Typography
      align="center"
      color="textSecondary"
      component="h6"
      gutterBottom
      variant="caption"
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
  const workspaceListDialog = hooks.useWorkspaceListDialog();
  const workspaces = hooks.useAllWorkspaces();
  const workspaceName = hooks.useWorkspaceName();
  const allTopics = hooks.useAllTopics();

  const handleClick = (name) => () => {
    switchWorkspace(name);
    workspaceListDialog.close();
  };

  const pickBrokerKey = (object) => {
    return {
      name: object?.brokerClusterKey?.name,
      group: object?.brokerClusterKey?.group,
    };
  };

  const workspaceCount = size(workspaces);

  return (
    <>
      <Dialog
        maxWidth="md"
        onClose={workspaceListDialog.close}
        open={workspaceListDialog.isOpen}
        showActions={false}
        testId="workspace-list-dialog"
        title={`Showing ${workspaceCount} ${
          workspaceCount > 1 ? 'workspaces' : 'workspace'
        }`}
      >
        <Wrapper>
          <Grid container spacing={2}>
            {workspaces.map((wk) => {
              const { name, nodeNames, lastModified } = wk;
              const avatarText = name.substring(0, 2).toUpperCase();
              const updatedText = moment(toNumber(lastModified)).fromNow();

              const isStable = isStableWorkspace(wk);
              const isActive = name === workspaceName;
              const brokerKey = { name, group: KIND.broker };
              const count = {
                nodes: size(nodeNames),
                pipelines: 0, // TODO: See the issue (https://github.com/oharastream/ohara/issues/3506)
                topics: size(
                  filter(allTopics, (topic) =>
                    isEqual(pickBrokerKey(topic), brokerKey),
                  ),
                ),
              };

              return (
                <Grid item key={name} xs={4}>
                  <Card
                    className={cx({
                      active: isActive,
                      inactive: !isActive,
                      unstable: !isStable,
                    })}
                  >
                    <CardHeader
                      avatar={
                        <Avatar className="workspace-icon">{avatarText}</Avatar>
                      }
                      subheader={`Updated: ${updatedText}`}
                      title={name}
                    />
                    <CardContent>
                      <Grid container spacing={2}>
                        <Grid item xs={6}>
                          <Statistic label="Nodes" value={count.nodes} />
                        </Grid>

                        {
                          /* Feature is disabled because it's not implemented in 0.9 */
                          false && (
                            <Grid item xs={4}>
                              <Statistic
                                label="Pipelines"
                                value={count.pipelines}
                              />
                            </Grid>
                          )
                        }
                        <Grid item xs={6}>
                          <Statistic label="Topics" value={count.topics} />
                        </Grid>
                      </Grid>
                    </CardContent>
                    <CardActions>
                      <Tooltip
                        className="call-to-action"
                        enterDelay={isActive ? 250 : 1000}
                        placement="top"
                        title={
                          isActive
                            ? "This is the workspace that you're at now"
                            : ''
                        }
                      >
                        <Button
                          disabled={isActive || !isStable}
                          onClick={handleClick(name)}
                          size="large"
                          startIcon={
                            isStable ? (
                              <InputIcon />
                            ) : (
                              <IconWrapper severity="warning">
                                <WarningIcon fontSize="small" />
                              </IconWrapper>
                            )
                          }
                        >
                          {isStable ? 'INTO WORKSPACE' : 'UNSTABLE WORKSPACE'}
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
