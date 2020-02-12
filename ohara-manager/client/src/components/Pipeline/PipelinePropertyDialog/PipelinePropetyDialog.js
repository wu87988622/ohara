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

import React, { useState, useEffect } from 'react';
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

import RenderDefinitions from './SettingDefinitions';
import { KIND } from 'const';
import {
  useConnectorState,
  useTopicState,
  useStreamState,
  useFileState,
  useWorkspace,
} from 'context';
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
  const {
    isOpen,
    handleClose,
    handleSubmit,
    data = {},
    maxWidth = 'md',
  } = props;
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

  const onSubmit = async values => {
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
      handleSubmit(
        {
          cell: cellData,
          topic: {
            ...topicCells.find(
              topic => topic.name === values.topicKeys[0].name,
            ),
          },
        },
        values,
        paperApi,
      );
      handleClose();
      return;
    }

    handleSubmit({ cell: cellData }, values, paperApi);
    handleClose();
  };

  const { RenderForm, formHandleSubmit, refs } = RenderDefinitions({
    topics: currentTopics,
    files: currentFiles,
    freePorts: get(currentWorker, 'freePorts', []),
    Definitions: groups.sort(),
    initialValues: targetCell,
    onSubmit,
  });

  const handleExpansionPanelChange = panel => (event, isExpanded) => {
    setExpanded(isExpanded ? panel : false);
  };

  const DialogTitle = params => {
    const { title, handleClose } = params;
    return (
      <StyleTitle disableTypography>
        <Typography variant="h4">{title}</Typography>
        {handleClose && (
          <StyleIconButton onClick={handleClose}>
            <CloseIcon />
          </StyleIconButton>
        )}
      </StyleTitle>
    );
  };

  useEffect(() => {
    if (selected) {
      //in react useEffect componentDidUpdate default event is scrollToTop,so we need setTimeout wait to scroll.
      setTimeout(() => {
        if (refs[selected]) {
          refs[selected].current.scrollIntoView({
            behavior: 'smooth',
            block: 'start',
          });
        }
      }, 100);
    }
  });

  const handleClick = async key => {
    setSelected(key);
  };

  return (
    <Dialog onClose={handleClose} open={isOpen} maxWidth={maxWidth} fullWidth>
      <DialogTitle handleClose={handleClose} title={title} />
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
        <RightBody>{RenderForm}</RightBody>
      </StyleMuiDialogContent>
      <StyleMuiDialogActions>
        <Button autoFocus onClick={() => formHandleSubmit()} color="primary">
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
  handleClose: PropTypes.func.isRequired,
  handleSubmit: PropTypes.func.isRequired,
};

export default PipelinePropertyDialog;
