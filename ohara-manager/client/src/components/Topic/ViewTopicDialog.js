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

import React, { useState } from 'react';
import { get, isEmpty, flatten, map } from 'lodash';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardContent from '@material-ui/core/CardContent';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import Divider from '@material-ui/core/Divider';
import Link from '@material-ui/core/Link';
import NumberFormat from 'react-number-format';

import TopicChip from './TopicChip';
import { FullScreenDialog, DeleteDialog } from 'components/common/Dialog';
import { Wrapper } from './ViewTopicDialogStyles';
import * as context from 'context';
import * as hooks from 'hooks';

const ViewTopicDialog = () => {
  const {
    isOpen: isDialogOpen,
    close: closeDialog,
    data: topic,
  } = context.useViewTopicDialog();
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const stopAndDeleteTopic = hooks.useStopAndDeleteTopicAction();
  const pipelines = hooks.usePipelines();

  if (!topic) return null;

  const handleDelete = async () => {
    const name = get(topic, 'name');
    stopAndDeleteTopic({ name });
    setIsConfirmOpen(false);
    closeDialog();
  };

  const isShared = topic?.tags?.isShared;
  const displayName = isShared ? topic?.name : topic?.tags?.displayName;

  const usedByPipelines = pipelines.filter(pipeline =>
    pipeline.objects.find(object => object.name === topic.name),
  );

  return (
    <FullScreenDialog
      title="View topic detail"
      open={isDialogOpen}
      handleClose={closeDialog}
      testId="view-topic-detail-dialog"
    >
      <Wrapper>
        <Grid container justify="space-between" alignItems="flex-end">
          <Grid item>
            <Typography component="h2" variant="overline" gutterBottom>
              Topics
            </Typography>
            <Typography component="div" variant="h3">
              {displayName}
            </Typography>
          </Grid>
          {isShared && (
            <Grid item>
              <Button
                variant="outlined"
                color="secondary"
                disabled={!isEmpty(usedByPipelines)}
                onClick={() => setIsConfirmOpen(true)}
              >
                Delete
              </Button>
              <DeleteDialog
                title="Delete topic?"
                content={`Are you sure you want to delete the topic: ${displayName} ? This action cannot be undone!`}
                open={isConfirmOpen}
                handleClose={() => setIsConfirmOpen(false)}
                handleConfirm={handleDelete}
                testId="view-topic-detail-delete-dialog"
              />
            </Grid>
          )}
        </Grid>
        <Grid container spacing={3} className="details">
          <Grid item xs={4}>
            <Card>
              <CardHeader title="Information" />
              <Divider />
              <CardContent>
                <Table>
                  <TableBody>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>{displayName}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Partitions</TableCell>
                      <TableCell>
                        {get(topic, 'numberOfPartitions', 0)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Replications</TableCell>
                      <TableCell>
                        {get(topic, 'numberOfReplications', 0)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Type</TableCell>
                      <TableCell>
                        <TopicChip isShared={isShared} />
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>State</TableCell>
                      <TableCell>{get(topic, 'state', 'Unknown')}</TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={4}>
            <Card>
              <CardHeader title="Metrics" />
              <Divider />
              <CardContent>
                <Table>
                  <TableBody>
                    {/* we need to display the metrics by each hostname
                        https://github.com/oharastream/ohara/issues/4495
                    */}
                    {flatten(
                      map(
                        get(topic, 'nodeMetrics', {}),
                        nodeMetric => nodeMetric.meters,
                      ),
                    ).map(metric => {
                      const document = get(metric, 'document');
                      const value = get(metric, 'value');
                      const unit = get(metric, 'unit');
                      return (
                        <TableRow>
                          <TableCell>{document}</TableCell>
                          <TableCell align="right">
                            <NumberFormat
                              value={value}
                              displayType="text"
                              thousandSeparator
                              renderText={value => (
                                <div>
                                  {value} {unit}
                                </div>
                              )}
                            />
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={4}>
            <Card>
              <CardHeader title="Used by pipelines" />
              <Divider />
              <CardContent>
                <Table>
                  <TableBody>
                    {usedByPipelines.map(pipeline => {
                      const pipelineName = get(pipeline, 'name');
                      return (
                        <TableRow key={pipelineName}>
                          <TableCell>{pipelineName}</TableCell>
                          {/* Disabled in 0.9 as the feature is not yet done */}
                          {false && (
                            <TableCell align="right">
                              <Link>Open</Link>
                            </TableCell>
                          )}
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Wrapper>
    </FullScreenDialog>
  );
};

export default ViewTopicDialog;
