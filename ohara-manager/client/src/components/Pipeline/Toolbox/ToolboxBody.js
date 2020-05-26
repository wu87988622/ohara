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
import List from '@material-ui/core/List';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';
import AddIcon from '@material-ui/icons/Add';
import Skeleton from '@material-ui/lab/Skeleton';
import { times } from 'lodash';

import * as context from 'context';
import ToolboxRedirectButton from './ToolboxRedirectButton';
import { AddTopicDialog } from 'components/Topic';

const ToolboxList = props => {
  const {
    toolboxHeight,
    toolboxBodyRef,
    panelAddButtonRef,
    panelSummaryRef,
    handleScroll,
    pipelineDispatch,
    expanded,
  } = props;
  const { open } = context.useWorkspaceSettingsDialog();

  const handleRedirect = pageName => {
    open(pageName);
  };

  return (
    <div
      className="toolbox-body"
      style={{ height: toolboxHeight ? toolboxHeight : 'auto' }}
      ref={toolboxBodyRef}
      onScroll={handleScroll}
    >
      <ExpansionPanel square expanded={expanded.source}>
        <ExpansionPanelSummary
          ref={panelSummaryRef}
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'source' })
          }
        >
          <Typography variant="subtitle1">Source</Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails className="detail">
          <List disablePadding>
            <div id="source-list" className="toolbox-list">
              {times(5, index => (
                <Skeleton
                  key={index}
                  animation="wave"
                  width={260}
                  height={40}
                />
              ))}
            </div>
          </List>

          <ToolboxRedirectButton
            buttonText="Add source connectors"
            onClick={() => handleRedirect('Worker plugins and shared jars')}
            ref={panelAddButtonRef}
          />
        </ExpansionPanelDetails>
      </ExpansionPanel>

      <ExpansionPanel square expanded={expanded.topic}>
        <ExpansionPanelSummary
          className="panel-title"
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'topic' })
          }
        >
          <Typography variant="subtitle1">Topic</Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails className="detail">
          <List disablePadding>
            <div id="topic-list" className="toolbox-list"></div>
          </List>

          <div className="add-button">
            <IconButton
              onClick={() => handleRedirect('Topics in this workspace')}
            >
              <AddIcon />
            </IconButton>
            <Typography variant="subtitle2">Add topics</Typography>
          </div>
        </ExpansionPanelDetails>
      </ExpansionPanel>

      <AddTopicDialog uniqueId="toolbox" />

      <ExpansionPanel square expanded={expanded.stream}>
        <ExpansionPanelSummary
          className="panel-title"
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'stream' })
          }
        >
          <Typography variant="subtitle1">Stream</Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails className="detail">
          <List disablePadding>
            <div id="stream-list" className="toolbox-list"></div>
          </List>

          <ToolboxRedirectButton
            buttonText="Add streams"
            onClick={() => handleRedirect('Stream jars')}
            ref={panelAddButtonRef}
          />
        </ExpansionPanelDetails>
      </ExpansionPanel>

      <ExpansionPanel square expanded={expanded.sink}>
        <ExpansionPanelSummary
          className="panel-title"
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'sink' })
          }
        >
          <Typography variant="subtitle1">Sink</Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails className="detail">
          <List disablePadding>
            <div id="sink-list" className="toolbox-list">
              {times(5, index => (
                <Skeleton
                  key={index}
                  animation="wave"
                  width={260}
                  height={40}
                />
              ))}
            </div>
          </List>

          <ToolboxRedirectButton
            buttonText="Add sink connectors"
            onClick={() => handleRedirect('Worker plugins and shared jars')}
            ref={panelAddButtonRef}
          />
        </ExpansionPanelDetails>
      </ExpansionPanel>
    </div>
  );
};

ToolboxList.propTypes = {
  toolboxHeight: PropTypes.number.isRequired,
  toolboxBodyRef: PropTypes.shape({ current: PropTypes.any }),
  panelAddButtonRef: PropTypes.shape({ current: PropTypes.any }),
  panelSummaryRef: PropTypes.shape({ current: PropTypes.any }),
  handleScroll: PropTypes.func.isRequired,
  pipelineDispatch: PropTypes.func.isRequired,
  expanded: PropTypes.shape({
    topic: PropTypes.bool.isRequired,
    source: PropTypes.bool.isRequired,
    sink: PropTypes.bool.isRequired,
    stream: PropTypes.bool.isRequired,
  }).isRequired,
};

export default ToolboxList;
