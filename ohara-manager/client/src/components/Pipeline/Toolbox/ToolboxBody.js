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
import Accordion from '@material-ui/core/Accordion';
import AccordionSummary from '@material-ui/core/AccordionSummary';
import AccordionDetails from '@material-ui/core/AccordionDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';
import AddIcon from '@material-ui/icons/Add';
import Skeleton from '@material-ui/lab/Skeleton';
import { times } from 'lodash';

import * as hooks from 'hooks';
import ToolboxRedirectButton from './ToolboxRedirectButton';

const ToolboxList = (props) => {
  const {
    toolboxHeight,
    toolboxBodyRef,
    panelAddButtonRef,
    panelSummaryRef,
    handleScroll,
    pipelineDispatch,
    expanded,
  } = props;
  const settingsDialog = hooks.useWorkspaceSettingsDialog();

  const handleRedirect = (pageName) => {
    settingsDialog.open({ pageName });
  };

  return (
    <div
      className="toolbox-body"
      onScroll={handleScroll}
      ref={toolboxBodyRef}
      style={{ height: toolboxHeight ? toolboxHeight : 'auto' }}
    >
      <Accordion expanded={expanded.source} id="source-panel" square>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'source' })
          }
          ref={panelSummaryRef}
        >
          <Typography variant="subtitle1">Source</Typography>
        </AccordionSummary>
        <AccordionDetails className="detail">
          <List disablePadding>
            <div className="toolbox-list" id="source-list">
              {times(5, (index) => (
                <Skeleton
                  animation="wave"
                  height={40}
                  key={index}
                  width={260}
                />
              ))}
            </div>
          </List>

          <ToolboxRedirectButton
            buttonText="Add source connectors"
            onClick={() => handleRedirect('Worker plugins and shared jars')}
            ref={panelAddButtonRef}
          />
        </AccordionDetails>
      </Accordion>

      <Accordion expanded={expanded.topic} id="topic-panel" square>
        <AccordionSummary
          className="panel-title"
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'topic' })
          }
        >
          <Typography variant="subtitle1">Topic</Typography>
        </AccordionSummary>
        <AccordionDetails className="detail">
          <List disablePadding>
            <div className="toolbox-list" id="topic-list"></div>
          </List>

          <div className="add-button">
            <IconButton
              onClick={() => handleRedirect('Topics in this workspace')}
            >
              <AddIcon />
            </IconButton>
            <Typography variant="subtitle2">Add topics</Typography>
          </div>
        </AccordionDetails>
      </Accordion>

      <Accordion expanded={expanded.stream} id="stream-panel" square>
        <AccordionSummary
          className="panel-title"
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'stream' })
          }
        >
          <Typography variant="subtitle1">Stream</Typography>
        </AccordionSummary>
        <AccordionDetails className="detail">
          <List disablePadding>
            <div className="toolbox-list" id="stream-list"></div>
          </List>

          <ToolboxRedirectButton
            buttonText="Add streams"
            onClick={() => handleRedirect('Stream jars')}
            ref={panelAddButtonRef}
          />
        </AccordionDetails>
      </Accordion>

      <Accordion expanded={expanded.sink} id="sink-panel" square>
        <AccordionSummary
          className="panel-title"
          expandIcon={<ExpandMoreIcon />}
          onClick={() =>
            pipelineDispatch({ type: 'setToolbox', payload: 'sink' })
          }
        >
          <Typography variant="subtitle1">Sink</Typography>
        </AccordionSummary>
        <AccordionDetails className="detail">
          <List disablePadding>
            <div className="toolbox-list" id="sink-list">
              {times(5, (index) => (
                <Skeleton
                  animation="wave"
                  height={40}
                  key={index}
                  width={260}
                />
              ))}
            </div>
          </List>

          <ToolboxRedirectButton
            buttonText="Add sink connectors"
            onClick={() => handleRedirect('Worker plugins and shared jars')}
            ref={panelAddButtonRef}
          />
        </AccordionDetails>
      </Accordion>
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
