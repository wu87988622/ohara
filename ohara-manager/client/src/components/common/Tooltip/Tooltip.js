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
import styled from 'styled-components';
import MuiTooltip from '@material-ui/core/Tooltip';

const StyledTooltip = styled(MuiTooltip)`
  /* Remove extra white space */
  line-height: 0;
`;

const Tooltip = props => {
  const { children, enterDelay = 1000, ...rest } = props;

  return (
    <StyledTooltip enterDelay={enterDelay} {...rest}>
      {/* The span is needed to properly display Tooltip even if the children component
      is disabled. See Mui's docs for more info about this: https://material-ui.com/components/tooltips/ */}
      <span>{children}</span>
    </StyledTooltip>
  );
};

Tooltip.propTypes = {
  children: PropTypes.node.isRequired,
  enterDelay: PropTypes.number,
};

export default Tooltip;
