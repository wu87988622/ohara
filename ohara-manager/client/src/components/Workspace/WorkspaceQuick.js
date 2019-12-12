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
import PropTypes from 'prop-types';

import Step from '@material-ui/core/Step';
import Card from '@material-ui/core/Card';
import { round, isUndefined } from 'lodash';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import Button from '@material-ui/core/Button';
import { Form, Field } from 'react-final-form';
import styled, { css } from 'styled-components';
import Stepper from '@material-ui/core/Stepper';
import TableRow from '@material-ui/core/TableRow';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import StepLabel from '@material-ui/core/StepLabel';
import CardContent from '@material-ui/core/CardContent';
import StepContent from '@material-ui/core/StepContent';

import {
  required,
  maxLength,
  validServiceName,
  composeValidators,
} from 'utils/validate';
import * as fileApi from 'api/fileApi';
import FileCard from './Card/FileCard';
import * as bkApi from 'api/brokerApi';
import * as wkApi from 'api/workerApi';
import * as zkApi from 'api/zookeeperApi';
import SelectCard from './Card/SelectCard';
import * as generate from 'utils/generate';
import WorkspaceCard from './Card/WorkspaceCard';

import { useNodeDialog } from 'context/NodeDialogContext';
import InputField from 'components/common/Form/InputField';
import { Progress } from 'components/common/Progress';
import FullScreenDialog from 'components/common/Dialog/FullScreenDialog';

const StyledPaper = styled(Paper)(
  ({ theme }) => css`
    min-height: ${theme.spacing(21)}px;
    margin-bottom: 16px;
  `,
);

const StyleStepper = styled(Stepper)`
  background-color: #f5f6fa;
`;

const StyleButton = styled(Button)`
  margin-right: 16px;
`;

const StyledTextField = styled(InputField)(
  ({ theme }) => css`
    margin: ${theme.spacing(3)}px 0 0 ${theme.spacing(2)}px;
    width: 95%;
  `,
);

const StyledTableRow = styled(TableRow)(
  ({ theme }) => css`
    &.MuiTableRow-root:nth-of-type(even) {
      background-color: ${theme.palette.grey[100]};
    }
  `,
);

const WorkspaceQuick = props => {
  const [activeStep, setActiveStep] = useState(0);
  const [files, setFiles] = useState([]);
  const [progressOpen, setProgressOpen] = useState(false);
  const [progressActiveStop, setProgressActiveStep] = useState(0);
  const {
    setIsOpen: setIsNodeDialogOpen,
    setHasSelect,
    setHasSave,
    selected,
    setSelected,
  } = useNodeDialog();

  const progressSteps = ['Zookeeper', 'Broker', 'Worker'];

  const steps = [
    'About this workspace',
    'Select nodes',
    'Upload or select worker plugins(Optional)',
    'Create this workspace',
  ];
  const { open, handelOpen } = props;
  if (open) {
    setHasSave(true);
    setHasSelect(true);
  }

  const handleNext = activeStep => {
    setActiveStep(activeStep + 1);
  };

  const handleBack = activeStep => {
    setActiveStep(activeStep - 1);
  };

  const removeNodeCard = node => {
    const newNodes = selected.filter(
      select => select[Object.keys(select)[0]] !== node[Object.keys(node)[0]],
    );
    setSelected(newNodes);
  };

  const onDrop = async (file, values) => {
    const result = await fileApi.create({
      group: values.workerName,
      file: file[0],
    });
    let fileInfo = {};
    if (!result.errors) {
      fileInfo = {
        ...result.data,
        file: result.data.name,
        name: result.data.name.replace(
          `.${result.data.name.split('.').pop()}`,
          '',
        ),
        'File Size': `${round(result.data.size / 1024, 2)}KiB`,
      };
    }

    const selectedIndex = files
      .map(select => select.file)
      .indexOf(fileInfo.name);
    let newSelected = [];
    if (selectedIndex === -1) {
      newSelected = newSelected.concat(files, fileInfo);
    } else if (selectedIndex === 0) {
      newSelected = newSelected.concat(files.slice(1));
    } else if (selectedIndex === files.length - 1) {
      newSelected = newSelected.concat(files.slice(0, -1));
    } else if (selectedIndex > 0) {
      newSelected = newSelected.concat(
        files.slice(0, selectedIndex),
        files.slice(selectedIndex + 1),
      );
    }
    setFiles(newSelected);
  };

  const removeFileCard = async file => {
    const newFiles = files.filter(
      select => select[Object.keys(select)[0]] !== file[Object.keys(file)[0]],
    );
    await fileApi.remove({
      group: file.group,
      name: file.file,
    });
    setFiles(newFiles);
  };

  const checkStepValue = (values, index) => {
    switch (index) {
      case 0:
        const error = composeValidators(
          required,
          validServiceName,
          maxLength(64),
        )(values.workerName);
        return !isUndefined(error);

      case 1:
        return !selected.length > 0;

      default:
        return false;
    }
  };

  const getRandoms = (nodes, n) => {
    let result = new Array(n);
    let len = nodes.length;
    let taken = new Array(len);

    while (n--) {
      let x = Math.floor(Math.random() * len);
      result[n] = nodes[x in taken ? taken[x] : x];
      taken[x] = --len in taken ? taken[len] : len;
    }
    return result;
  };

  const createZk = async nodeNames => {
    const zkCreateRes = await zkApi.create({
      name: generate.serviceName({ prefix: 'zk' }),
      nodeNames:
        nodeNames.length > 3
          ? getRandoms(nodeNames, 3)
          : getRandoms(nodeNames, 1),
    });
    const zkStartRes = zkCreateRes.errors
      ? {}
      : await zkApi.start({
          name: zkCreateRes.data.settings.name,
          group: zkCreateRes.data.settings.group,
        });

    return zkStartRes.errors ? {} : zkStartRes.data;
  };

  const createBk = async params => {
    const { nodeNames, zookeeper } = params;
    const bkCreateRes = await bkApi.create({
      name: generate.serviceName({ prefix: 'bk' }),
      nodeNames,
      zookeeperClusterKey: {
        name: zookeeper.settings.name,
        group: zookeeper.settings.group,
      },
    });
    const bkStartRes = bkCreateRes.errors
      ? {}
      : await bkApi.start({
          name: bkCreateRes.data.settings.name,
          group: bkCreateRes.data.settings.group,
        });

    return bkStartRes.errors ? {} : bkStartRes.data;
  };

  const createWk = async params => {
    const { nodeNames, broker, workerName, plugins } = params;
    const wkCreateRes = await wkApi.create({
      name: workerName,
      nodeNames,
      pluginKeys: plugins,
      brokerClusterKey: {
        name: broker.settings.name,
        group: broker.settings.group,
      },
    });
    const wkStartRes = wkCreateRes.errors
      ? {}
      : await wkApi.start({
          name: wkCreateRes.data.settings.name,
          group: wkCreateRes.data.settings.group,
        });

    return wkStartRes;
  };

  const createQuickWorkspace = async values => {
    const { workerName } = values;
    const nodeNames = selected.map(select => select.name);
    const plugins = files.map(file => {
      return {
        name: file.file,
        group: file.group,
      };
    });

    setProgressOpen(true);
    const zk = await createZk(nodeNames);
    setProgressActiveStep(1);
    const bk = await createBk({ nodeNames, zookeeper: zk });
    setProgressActiveStep(2);
    await createWk({ nodeNames, broker: bk, workerName, plugins });
    setProgressActiveStep(3);
    handelOpen(false);
  };

  const getStepContent = (step, values) => {
    switch (step) {
      case 0:
        return (
          <Field
            type="text"
            name="workerName"
            label="Workspace name"
            margin="normal"
            helperText="Assistive text"
            component={StyledTextField}
            autoFocus
            required
            validate={composeValidators(
              required,
              validServiceName,
              maxLength(64),
            )}
          />
        );
      case 1:
        return (
          <Card>
            <CardContent>{'Workspace nodes'}</CardContent>
            {selected.length > 0 ? (
              <>
                {WorkspaceCard({
                  onClick: setIsNodeDialogOpen,
                  title: 'Select nodes',
                  content: 'Click here to select nodes',
                  sm: true,
                })}
                {selected.map(node => {
                  return SelectCard({
                    rows: node,
                    handleClose: removeNodeCard,
                  });
                })}
              </>
            ) : (
              <CardContent>
                {WorkspaceCard({
                  onClick: setIsNodeDialogOpen,
                  title: 'Select nodes',
                  content: 'Click here to select nodes',
                })}
              </CardContent>
            )}
          </Card>
        );
      case 2:
        return (
          <Card>
            <CardContent>{'Worker plugins'}</CardContent>
            {files.length > 0 ? (
              <>
                {FileCard({
                  handelDrop: onDrop,
                  title: 'Add worker plugins',
                  content: 'Drop files here or click to select files to upload',
                  sm: true,
                  values,
                })}
                {files.map(file => {
                  return SelectCard({
                    rows: file,
                    handleClose: removeFileCard,
                    filterKey: ['url', 'lastModified', 'group', 'size'],
                  });
                })}
              </>
            ) : (
              <CardContent>
                {FileCard({
                  handelDrop: onDrop,
                  title: 'Add worker plugins',
                  content: 'Drop files here or click to select files to upload',
                  values,
                })}
              </CardContent>
            )}
          </Card>
        );
      case 3:
        return (
          <Card>
            <CardContent>{'Summary'}</CardContent>
            <CardContent>
              <Paper>
                <Table>
                  <TableBody>
                    <StyledTableRow>
                      <TableCell>{'Workspace Name'}</TableCell>
                      <TableCell>{values.workerName}</TableCell>
                    </StyledTableRow>
                    <StyledTableRow>
                      <TableCell>{'Node Names'}</TableCell>
                      <TableCell>
                        {selected.map(selected => selected.name).join(',')}
                      </TableCell>
                    </StyledTableRow>
                    <StyledTableRow>
                      <TableCell>{'Plugins'}</TableCell>
                      <TableCell>
                        {files.map(file => file.file).join(',')}
                      </TableCell>
                    </StyledTableRow>
                  </TableBody>
                </Table>
              </Paper>
            </CardContent>
          </Card>
        );

      default:
        return 'Unknown step';
    }
  };
  return (
    <>
      <Form
        onSubmit={createQuickWorkspace}
        initialValues={{}}
        render={({ handleSubmit, form, values }) => {
          return (
            <FullScreenDialog
              title="Create workspace - Quick"
              open={open}
              handleClose={() => {
                setHasSelect(false);
                handelOpen(false);
              }}
              children={
                <form onSubmit={handleSubmit}>
                  <StyleStepper activeStep={activeStep} orientation="vertical">
                    {steps.map((label, index) => (
                      <Step key={label}>
                        <StepLabel>{label}</StepLabel>
                        <StepContent>
                          <StyledPaper>
                            {getStepContent(index, values)}
                          </StyledPaper>
                          <StyleButton
                            variant="contained"
                            color="primary"
                            onClick={() =>
                              activeStep === steps.length - 1
                                ? handleSubmit(values)
                                : handleNext(activeStep)
                            }
                            disabled={checkStepValue(values, activeStep)}
                          >
                            {activeStep === steps.length - 1
                              ? 'Finish'
                              : 'Next'}
                          </StyleButton>
                          <Button
                            disabled={activeStep === 0}
                            onClick={() => handleBack(activeStep)}
                          >
                            Back
                          </Button>
                        </StepContent>
                      </Step>
                    ))}
                  </StyleStepper>
                </form>
              }
            />
          );
        }}
      />
      <Progress
        open={progressOpen}
        steps={progressSteps}
        createTitle={'Create Workspace'}
        activeStep={progressActiveStop}
      />
    </>
  );
};

WorkspaceQuick.propTypes = {
  open: PropTypes.bool.isRequired,
  handelOpen: PropTypes.func.isRequired,
};

export default WorkspaceQuick;
