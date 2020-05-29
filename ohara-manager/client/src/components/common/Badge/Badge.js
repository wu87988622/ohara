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
import { includes } from 'lodash';
import clsx from 'classnames';
import styled, { css } from 'styled-components';
import MuiBadge from '@material-ui/core/Badge';

const Wrapper = styled.div(
  ({ theme }) => css`
    .badge-color-success .MuiBadge-badge {
      color: ${theme.palette.common.white};
      background-color: ${theme.palette.success.main};
    }
    .badge-color-info .MuiBadge-badge {
      color: ${theme.palette.common.white};
      background-color: ${theme.palette.info.main};
    }
    .badge-color-warning .MuiBadge-badge {
      color: ${theme.palette.common.white};
      background-color: ${theme.palette.warning.main};
    }
  `,
);

const ENHANCE_COLORS = ['success', 'info', 'warning'];

Badge.propTypes = {
  color: PropTypes.oneOf([
    'default',
    'error',
    'primary',
    'secondary',
    ...ENHANCE_COLORS,
  ]),
};

Badge.defaultProps = {
  color: 'default',
};

function Badge(props) {
  const { color } = props;

  return (
    <Wrapper>
      <MuiBadge
        {...props}
        className={clsx({
          'badge-color-success': color === 'success',
          'badge-color-info': color === 'info',
          'badge-color-warning': color === 'warning',
        })}
        color={includes(ENHANCE_COLORS, color) ? 'default' : color}
      />
    </Wrapper>
  );
}

export default Badge;
