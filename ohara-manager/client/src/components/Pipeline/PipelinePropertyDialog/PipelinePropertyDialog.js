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

import React, { useRef, useState } from 'react';
import PropTypes from 'prop-types';
import { find, filter, isEmpty, capitalize, has, isArray, get } from 'lodash';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import CloseIcon from '@material-ui/icons/Close';
import ListItem from '@material-ui/core/ListItem';
import Typography from '@material-ui/core/Typography';
import ListItemText from '@material-ui/core/ListItemText';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import FilterListIcon from '@material-ui/icons/FilterList';
import InputAdornment from '@material-ui/core/InputAdornment';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';

import PipelinePropertyForm from './PipelinePropertyForm';
import { KIND } from 'const';
import {
  useConnectorState,
  useTopicState,
  useStreamState,
  useFileState,
  useWorkspace,
} from 'context';
import PipelinePropertySpeedDial from './PipelinePropertySpeedDial';
import {
  StyleTitle,
  StyleIconButton,
  StyleMuiDialogContent,
  StyleMuiDialogActions,
  LeftBody,
  RightBody,
  StyleFilter,
  StyleExpansionPanel,
} from './PipelinePropertyDialogStyles';

const PipelinePropertyDialog = props => {
  const { isOpen, onClose, onSubmit, data = {}, maxWidth = 'md' } = props;
  const {
    title = '',
    classInfo = {},
    cellData = { kind: '' },
    paperApi,
  } = data;
  const { kind } = cellData;
  const [expanded, setExpanded] = useState(null);
  const [selected, setSelected] = useState(null);
  const { currentWorker } = useWorkspace();
  const { data: currentConnectors } = useConnectorState();
  const { data: currentStreams } = useStreamState();
  const { data: currentTopics } = useTopicState();
  const { data: currentFiles } = useFileState();

  const formRef = useRef(null);

  let targetCell;
  switch (kind) {
    case KIND.source:
    case KIND.sink:
      targetCell = currentConnectors.find(
        connector =>
          connector.className === classInfo.className &&
          connector.name === cellData.name,
      );
      break;
    case KIND.stream:
      targetCell = currentStreams.find(stream => stream.name === cellData.name);
      break;

    default:
      break;
  }

  const groupBy = array => {
    if (!array) return [];
    let groups = {};
    const getGroup = item => {
      return [item.group];
    };
    array.forEach(obj => {
      let group = JSON.stringify(getGroup(obj));
      groups[group] = groups[group] || [];
      groups[group].push(obj);
    });

    return Object.keys(groups).map(group => {
      return groups[group];
    });
  };

  const groups = groupBy(classInfo.settingDefinitions);

  const getTopicWithKey = (values, key) => {
    if (values[key] === 'Please select...' || isArray(values[key])) return;
    const isPipelineOnlyTopic = !isEmpty(
      filter(values[key], topicKey => topicKey.startsWith('T')),
    );
    if (isPipelineOnlyTopic) {
      const pipelineOnlyTopic = find(
        currentTopics,
        topic => topic.tags.displayName === values[key],
      );
      values[key] = [
        { name: pipelineOnlyTopic.name, group: pipelineOnlyTopic.group },
      ];
    } else {
      const publicTopic = currentTopics.filter(
        topic => topic.name === values[key],
      )[0];
      values[key] = [{ name: publicTopic.name, group: publicTopic.group }];
    }
  };

  const handleSubmit = async values => {
    const topicCells = paperApi.getCells(KIND.topic);
    Object.keys(values).forEach(key => {
      switch (key) {
        case 'topicKeys':
          getTopicWithKey(values, key);
          break;
        case 'to':
          getTopicWithKey(values, key);
          break;
        case 'from':
          getTopicWithKey(values, key);
          break;
        default:
          break;
      }
    });
    if (has(values, 'topicKeys') || has(values, 'to') || has(values, 'from')) {
      onSubmit(
        {
          cell: cellData,
          topic: {
            ...topicCells.find(
              topic => topic.name === get(values, 'topicKeys[0].name', null),
            ),
            ...topicCells.find(
              topic => topic.name === get(values, 'to[0].name', null),
            ),
            ...topicCells.find(
              topic => topic.name === get(values, 'from[0].name', null),
            ),
          },
        },
        values,
        paperApi,
      );
      onClose();
      return;
    }

    onSubmit({ cell: cellData }, values, paperApi);
    onClose();
  };

  const handleExpansionPanelChange = panel => (event, isExpanded) => {
    setExpanded(isExpanded ? panel : false);
  };

  const DialogTitle = params => {
    const { title, onClose } = params;
    return (
      <StyleTitle disableTypography>
        <Typography variant="h4">{title}</Typography>
        {onClose && (
          <StyleIconButton onClick={onClose}>
            <CloseIcon />
          </StyleIconButton>
        )}
      </StyleTitle>
    );
  };

  const handleClick = async key => {
    setSelected(key);
    setTimeout(() => {
      if (formRef.current) formRef.current.scrollIntoView(key);
    }, 100);
  };

  return (
    <Dialog onClose={onClose} open={isOpen} maxWidth={maxWidth} fullWidth>
      <DialogTitle onClose={onClose} title={title} />
      <StyleMuiDialogContent dividers>
        <LeftBody>
          <StyleFilter
            placeholder="Quick filter"
            variant="outlined"
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <FilterListIcon />
                </InputAdornment>
              ),
            }}
          />
          <div>
            {groups.sort().map((group, index) => {
              const title = group[0].group;
              const defs = group.filter(def => !def.internal);

              if (defs.length > 0) {
                return (
                  <StyleExpansionPanel
                    expanded={
                      expanded === title || (index === 0 && expanded === null)
                    }
                    onChange={handleExpansionPanelChange(title)}
                    key={title}
                  >
                    <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
                      <Typography>{capitalize(title)}</Typography>
                    </ExpansionPanelSummary>
                    <ExpansionPanelDetails>
                      <div>
                        {defs.map((def, index) => {
                          return (
                            <ListItem
                              className="nested"
                              button
                              key={def.key}
                              selected={
                                def.key === selected ||
                                (selected === null && index === 0)
                              }
                              onClick={() => handleClick(def.key)}
                            >
                              <ListItemText primary={def.displayName} />
                            </ListItem>
                          );
                        })}
                      </div>
                    </ExpansionPanelDetails>
                  </StyleExpansionPanel>
                );
              } else {
                return null;
              }
            })}
          </div>
        </LeftBody>
        <RightBody>
          <PipelinePropertyForm
            definitions={groups.sort()}
            files={currentFiles}
            freePorts={get(currentWorker, 'freePorts', [])}
            initialValues={targetCell}
            onSubmit={handleSubmit}
            ref={formRef}
            topics={currentTopics}
          />
        </RightBody>
        <div className="speed-dial">
          <PipelinePropertySpeedDial formRef={formRef} />
        </div>
      </StyleMuiDialogContent>
      <StyleMuiDialogActions>
        <Button
          autoFocus
          onClick={() => formRef.current.submit()}
          color="primary"
        >
          Save changes
        </Button>
      </StyleMuiDialogActions>
    </Dialog>
  );
};

PipelinePropertyDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  data: PropTypes.object,
  maxWidth: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
};

export default PipelinePropertyDialog;
