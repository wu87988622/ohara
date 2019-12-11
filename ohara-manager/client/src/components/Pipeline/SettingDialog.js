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
import { capitalize } from 'lodash';
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

import { RenderDefinitions } from 'components/common/Definitions';

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
  () => css`
    float: left;
    height: 600px;
    width: 256px;
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

    & > form > .MuiPaper-elevation2 {
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
  const { title = '', classInfo = {} } = data;
  const [expanded, setExpanded] = React.useState(false);

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

  const Defintions = RenderDefinitions({
    Definitions: classInfo.settingDefinitions,
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
            {groups.map(group => {
              const title = group[0].group;
              const defs = group.filter(def => !def.internal);
              if (defs.length > 0) {
                return (
                  <StyleExpansionPanel
                    expanded={expanded === title}
                    onChange={handleExpansionPanelChange(title)}
                    key={title}
                  >
                    <ExpansionPanelSummary expandIcon={<ExpandMoreIcon />}>
                      <Typography>{capitalize(title)}</Typography>
                    </ExpansionPanelSummary>
                    <ExpansionPanelDetails>
                      <StyleList>
                        {defs.map(def => {
                          return (
                            <ListItem button key={def.key}>
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
        <RightBody>{Defintions}</RightBody>
      </StyleMuiDialogContent>
      <StyleMuiDialogActions>
        <Button autoFocus onClick={handleClose} color="primary">
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
