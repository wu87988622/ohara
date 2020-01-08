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
import styled, { css } from 'styled-components';
import { isObject, has, get } from 'lodash';
import Toolbar from '@material-ui/core/Toolbar';
import IconButton from '@material-ui/core/IconButton';
import CloseIcon from '@material-ui/icons/Close';
import Card from '@material-ui/core/Card';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import CardContent from '@material-ui/core/CardContent';
import Typography from '@material-ui/core/Typography';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';

import { Percentage } from 'components/common/Progress';

const StyledDiv = styled.div(
  ({ theme }) => css`
    display: flex;
    justify-content: space-between;
    margin-bottom: ${theme.spacing(1)}px;
  `,
);

const StyledToolbar = styled(Toolbar)(
  ({ theme }) => css`
    min-height: ${theme.spacing(5)}px;
    padding-left: 0;
    padding-right: 0;
    margin-bottom: ${theme.spacing(1)}px;
  `,
);

const StyledNodeCardTitle = styled(Typography)`
  text-overflow: ellipsis;
  overflow: hidden;
  flex: 1;
`;

const StyledCard = styled(Card)(
  ({ theme }) => css`
    width: ${theme.spacing(33)}px;
    min-height: ${theme.spacing(25)}px;
    float: left;
    margin: ${theme.spacing(1.5)}px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  `,
);

const StyledExpansionPanel = styled(ExpansionPanel)`
  &.MuiExpansionPanel-root.Mui-expanded {
    margin: 0;
  }
`;
const StyledExpansionPanelSummary = styled(ExpansionPanelSummary)(
  ({ theme }) => css`
    padding: 0 ${theme.spacing(1)}px 0 ${theme.spacing(2)}px;

    &.MuiExpansionPanelSummary-root.Mui-expanded {
      min-height: 0;
      height: ${theme.spacing(4)}px;
    }
    .MuiExpansionPanelSummary-content.Mui-expanded {
      margin: 0;
    }
  `,
);

const StyledExpansionPanelDetails = styled(ExpansionPanelDetails)(
  ({ theme }) => css`
    padding: ${theme.spacing(0.5)}px ${theme.spacing(1)}px
      ${theme.spacing(0.5)}px ${theme.spacing(2)}px;
  `,
);

const StyledTypography = styled(Typography)`
  text-overflow: ellipsis;
  overflow: hidden;
  flex: 1;
`;

const StyledTypographyVersion = styled(Typography)`
  text-overflow: ellipsis;
  overflow: hidden;
`;

const groupBy = (source, key) => {
  return source.reduce((acc, cur) => {
    (acc[cur[key]] = acc[cur[key]] || []).push(cur);
    return acc;
  }, {});
};

const SelectCard = props => {
  const { rows, handleClose, filterKey = [] } = props;
  return (
    <StyledCard key={rows[Object.keys(rows)[0]]}>
      <CardContent>
        {
          <StyledToolbar>
            <StyledNodeCardTitle variant="h5">{rows.name}</StyledNodeCardTitle>
            <IconButton size="small" onClick={() => handleClose(rows)}>
              <CloseIcon />
            </IconButton>
          </StyledToolbar>
        }
        {Object.keys(rows)
          .filter(key => !filterKey.includes(key))
          .filter(key => !isObject(rows[key]) && key !== 'name')
          .map(key => {
            return (
              <StyledDiv key={key + rows[key]}>
                <Typography variant="subtitle2">
                  {key.charAt(0).toUpperCase() + key.slice(1)}
                </Typography>
                <div>{Percentage(rows[key], '|')}</div>
              </StyledDiv>
            );
          })}
      </CardContent>
      {has(rows, 'classInfos') && (
        <>
          {Object.keys(groupBy(rows.classInfos, 'classType')).map(key => {
            const array = groupBy(rows.classInfos, 'classType')[key];
            return (
              <StyledExpansionPanel key={key}>
                <StyledExpansionPanelSummary
                  expandIcon={<ExpandMoreIcon />}
                  variant="subtitle2"
                >
                  <StyledTypography variant="subtitle2">{key}</StyledTypography>
                  <div>{array.length}</div>
                </StyledExpansionPanelSummary>
                {array.map(classes => {
                  return (
                    <StyledExpansionPanelDetails key={classes.className}>
                      <StyledTypography variant="subtitle2">
                        {classes.className.split('.').pop()}
                      </StyledTypography>
                      <StyledTypographyVersion variant="subtitle2">
                        {get(
                          classes.settingDefinitions.filter(
                            def => def.key === 'version',
                          )[0],
                          'defaultValue',
                          '',
                        )}
                      </StyledTypographyVersion>
                    </StyledExpansionPanelDetails>
                  );
                })}
              </StyledExpansionPanel>
            );
          })}
        </>
      )}
    </StyledCard>
  );
};

SelectCard.propTypes = {
  rows: PropTypes.array.isRequired,
  handleClose: PropTypes.func.isRequired,
  filterKey: PropTypes.array,
};

export default SelectCard;
