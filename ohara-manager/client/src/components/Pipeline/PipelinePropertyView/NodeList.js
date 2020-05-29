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
import Typography from '@material-ui/core/Typography';
import cx from 'classnames';
import styled, { css } from 'styled-components';

import PropertyField from './PipelinePropertyViewField';

const Wrapper = styled.div(
  ({ theme }) => css`
    h6 {
      margin-bottom: ${theme.spacing(2)}px;
    }

    .is-failed {
      color: ${theme.palette.error.main};
      text-decoration: underline;
      cursor: pointer;

      &:hover {
        text-decoration: none;
      }
    }
  `,
);

const NodeList = ({ heading, list, onErrorTextClick }) => {
  return (
    <Wrapper>
      <Typography variant="h6">{heading}</Typography>
      {list
        .sort((a, b) => a.nodeName.localeCompare(b.nodeName))
        .map(item => {
          const { nodeName, state, master, error = null } = item;

          // Since `PropertyField` only renders value in pair "abc:efg", we just
          // need the first name to be rendered
          const className = cx('node-status', { 'is-failed': error });
          const onClick = error
            ? onErrorTextClick({
                title: `${nodeName} ${master ? 'master' : 'slave'} errors`,
                message: { error },
              })
            : undefined;

          return (
            <PropertyField
              key={nodeName}
              label={''}
              slot={
                <Typography
                  className={className}
                  component="span"
                  onClick={onClick}
                  variant="body2"
                >
                  {state}
                </Typography>
              }
              value={nodeName}
            />
          );
        })}
    </Wrapper>
  );
};

NodeList.propTypes = {
  onErrorTextClick: PropTypes.func.isRequired,
  heading: PropTypes.string.isRequired,
  list: PropTypes.arrayOf(
    PropTypes.shape({
      nodeName: PropTypes.string.isRequired,
      state: PropTypes.string.isRequired,
      master: PropTypes.bool.isRequired,
      error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    }).isRequired,
  ).isRequired,
};

export default NodeList;
