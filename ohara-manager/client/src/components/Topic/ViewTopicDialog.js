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
import { useParams } from 'react-router-dom';
import { get, isEmpty } from 'lodash';
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

import * as topicApi from 'api/topicApi';
import { useViewTopic } from 'context/ViewTopicContext';
import { useSnackbar } from 'context/SnackbarContext';
import { useTopic } from 'context/TopicContext';
import { useWorkspace } from 'context/WorkspaceContext';
import { FullScreenDialog, DeleteDialog } from 'components/common/Dialog';
import { Wrapper } from './ViewTopicDialogStyles';

const ViewTopicDialog = () => {
  const { isOpen, setIsOpen, topic } = useViewTopic();
  const [isDeleting, setIsDeleting] = useState(false);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const showMessage = useSnackbar();

  const { findByWorkspaceName } = useWorkspace();
  const { workspaceName } = useParams();
  const currentWorkspace = findByWorkspaceName(workspaceName);
  const { doFetch: fetchTopics } = useTopic();

  const topicGroup = get(topic, 'settings.group');
  const topicName = get(topic, 'settings.name');
  const partitions = get(topic, 'settings.numberOfPartitions');
  const replications = get(topic, 'settings.numberOfReplications');
  const state = get(topic, 'state', 'Unknown');
  const metrics = get(topic, 'metrics.meters', []);
  const usedByPipelines = []; // TODO: fetch pipelines

  const handleDelete = async () => {
    setIsDeleting(true);
    const removeTopicResponse = await topicApi.remove({
      name: topicName,
      group: topicGroup,
    });
    const isRemoved = !isEmpty(removeTopicResponse);

    // Failed to remove, show a custom error message here and keep the
    // dialog open
    if (!isRemoved) {
      showMessage(`Failed to delete topic ${topicName}`);
      setIsDeleting(false);
      return;
    }

    // Topic successfully deleted, display success message,
    // update topic list and close the dialog
    showMessage(`Successfully deleted topic ${topicName}`);
    await fetchTopics(currentWorkspace.settings.name);

    setIsDeleting(false);
    setIsConfirmOpen(false);
    setIsOpen(false);
  };

  return (
    <FullScreenDialog
      title="View topic detail"
      open={isOpen}
      handleClose={() => setIsOpen(false)}
      loading={isDeleting}
    >
      <Wrapper>
        <Grid container justify="space-between" alignItems="flex-end">
          <Grid item>
            <Typography component="h2" variant="overline" gutterBottom>
              Topics
            </Typography>
            <Typography component="h1" variant="h3">
              {topicName}
            </Typography>
          </Grid>
          <Grid item>
            <Button
              variant="outlined"
              color="secondary"
              disabled={isEmpty(topicName) || !isEmpty(usedByPipelines)}
              onClick={() => setIsConfirmOpen(true)}
            >
              Delete
            </Button>
            <DeleteDialog
              title="Delete topic?"
              content={`Are you sure you want to delete the topic: ${topicName} ? This action cannot be undone!`}
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
              <CardHeader title="Information" />
              <Divider />
              <CardContent>
                <Table>
                  <TableBody>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>{topicName}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Partitions</TableCell>
                      <TableCell>{partitions}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Replications</TableCell>
                      <TableCell>{replications}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Type</TableCell>
                      <TableCell>{}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>State</TableCell>
                      <TableCell>{state}</TableCell>
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
                    {metrics.map(metric => {
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
                        <TableRow>
                          <TableCell>{pipelineName}</TableCell>
                          <TableCell align="right">
                            <Link>Open</Link>
                          </TableCell>
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
