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
import { find, some, filter, isEmpty, capitalize } from 'lodash';
import List from '@material-ui/core/List';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import styled, { css } from 'styled-components';
import CloseIcon from '@material-ui/icons/Close';
import ListItem from '@material-ui/core/ListItem';
import TextField from '@material-ui/core/TextField';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import ListItemText from '@material-ui/core/ListItemText';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import FilterListIcon from '@material-ui/icons/FilterList';
import MuiDialogTitle from '@material-ui/core/DialogTitle';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import InputAdornment from '@material-ui/core/InputAdornment';
import MuiDialogContent from '@material-ui/core/DialogContent';
import MuiDialogActions from '@material-ui/core/DialogActions';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';

import RenderDefinitions from './SettingDefinitions';
import { useConnectorState, useConnectorActions, useTopicState } from 'context';

const StyleTitle = styled(MuiDialogTitle)(
  ({ theme }) => css`
    margin: 0;
    padding: ${theme.spacing(2)}px;
  `,
);

const StyleIconButton = styled(IconButton)(
  ({ theme }) => css`
    position: absolute;
    right: ${theme.spacing(1)}px;
    top: ${theme.spacing(1)}px;
    color: ${theme.palette.grey[500]};
  `,
);

const StyleMuiDialogContent = styled(MuiDialogContent)(
  ({ theme }) => css`
    height: 100%;
    padding: ${theme.spacing(2)}px;
  `,
);

const StyleMuiDialogActions = styled(MuiDialogActions)(
  ({ theme }) => css`
    margin: 0;
    padding: ${theme.spacing(1)}px;
  `,
);

const LeftBody = styled.div(
  ({ theme }) => css`
    float: left;
    height: 600px;
    width: 256px;

    .nested {
      padding-left: ${theme.spacing(3)}px;

      .MuiListItemText-root {
        padding-left: ${theme.spacing(3)}px;
      }

      ::before {
        content: '';
        left: ${theme.spacing(3)}px;
        top: 0;
        bottom: 0;
        position: absolute;
        width: ${theme.spacing(0.25)}px;
        background-color: ${theme.palette.grey[300]};
      }

      :first-child::before {
        margin-top: ${theme.spacing(1.5)}px;
      }

      :last-child::before {
        margin-bottom: ${theme.spacing(1.5)}px;
      }

      &.Mui-selected {
        background-color: white;

        .MuiListItemText-root {
          border-left: ${theme.palette.primary[600]} ${theme.spacing(0.25)}px
            solid;
          z-index: 0;
        }
      }
    }
  `,
);
const RightBody = styled.div(
  ({ theme }) => css`
    float: left;
    height: 600px;
    width: 656px;
    overflow: scroll;
    margin-left: ${theme.spacing(2)}px;
    padding-right: ${theme.spacing(2)}px;

    & > form > * {
      margin: ${theme.spacing(0, 0, 3, 2)};
    }

    & > form > div > .MuiPaper-elevation2 {
      padding-left: ${theme.spacing(1)}px;
      margin-left: ${theme.spacing(1)}px;
    }
  `,
);

const StyleFilter = styled(TextField)(
  ({ theme }) => css`
    width: 100%;
    margin-bottom: ${theme.spacing(2)}px;
  `,
);

const StyleExpansionPanel = styled(ExpansionPanel)(
  () => css`
    &.MuiExpansionPanel-root.Mui-expanded {
      margin: 0;
    }
  `,
);

const StyleList = styled(List)(
  () => css`
    width: 100%;
  `,
);

const SettingDialog = props => {
  const { open, handleClose, data = {}, maxWidth = 'md' } = props;
  const { title = '', classInfo = {}, name: connectorName } = data;
  const [expanded, setExpanded] = useState(null);
  const [selected, setSelected] = useState(null);
  const { data: currentConnectors } = useConnectorState();
  const { data: currentTopics } = useTopicState();
  const { updateConnector } = useConnectorActions();
  const targetConnector = currentConnectors.find(
    connector =>
      connector.className === classInfo.className &&
      connector.name === connectorName,
  );

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

  const StyleDiv = styled.div`
    max-height: 540px;
    overflow: scroll;
    padding: 1px;
  `;

  const onSubmit = async values => {
    if (!isEmpty(values.topicKeys)) {
      // topicKeys could be an empty array or single string
      // May need refactoring to only one type (array)
      // But it's ok to use filter function here
      if (
        !isEmpty(filter(values.topicKeys, topicKey => topicKey.startsWith('T')))
      ) {
        const privateTopic = find(currentTopics, topic =>
          some(
            values.topicKeys,
            topicKey => topicKey === topic.tags.displayName,
          ),
        );

        values.topicKeys = [
          { name: privateTopic.name, group: privateTopic.group },
        ];
      } else {
        const publicTopic = currentTopics.filter(
          topic => topic.name === values.topicKeys,
        )[0];
        values.topicKeys = [
          { name: publicTopic.name, group: publicTopic.group },
        ];
      }
    }
    await updateConnector({
      name: targetConnector.name,
      group: targetConnector.group,
      ...values,
    });
    handleClose();
  };

  const { RenderForm, formHandleSubmit, refs } = RenderDefinitions({
    topics: currentTopics,
    Definitions: groups.sort(),
    initialValues: targetConnector,
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
    <Dialog onClose={handleClose} open={open} maxWidth={maxWidth} fullWidth>
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
          <StyleDiv>
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
                      <StyleList>
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
                      </StyleList>
                    </ExpansionPanelDetails>
                  </StyleExpansionPanel>
                );
              } else {
                return null;
              }
            })}
          </StyleDiv>
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

SettingDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  data: PropTypes.object,
  maxWidth: PropTypes.string,
  handleClose: PropTypes.func.isRequired,
};

export default SettingDialog;
