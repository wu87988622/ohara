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
import StorageIcon from '@material-ui/icons/Storage';
import Typography from '@material-ui/core/Typography';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import PropertyField from './PipelinePropertyViewField';

const NodePanel = props => {
  const { tasksStatus } = props;
  const [isExpanded, setIsExpanded] = React.useState(false);

  if (tasksStatus.length === 0) return null;

  return (
    <>
      <ExpansionPanel defaultExpanded={true} expanded={isExpanded}>
        <ExpansionPanelSummary
          onClick={() => setIsExpanded(prevState => !prevState)}
          expandIcon={<ExpandMoreIcon />}
        >
          <StorageIcon fontSize="small" />
          <Typography className="section-title" variant="h5">
            Nodes
          </Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          {tasksStatus
            // UI currently doesn't support displaying master node.
            .filter(node => !node.master)
            .map(node => {
              const { nodeName, state } = node;
              return (
                <PropertyField
                  key={nodeName}
                  label="Name"
                  value={nodeName}
                  slot={
                    <Typography
                      variant="body2"
                      className="node-status"
                      component="span"
                    >
                      {state}
                    </Typography>
                  }
                />
              );
            })}
        </ExpansionPanelDetails>
      </ExpansionPanel>
    </>
  );
};

NodePanel.propTypes = {
  tasksStatus: PropTypes.array,
};

export default NodePanel;
