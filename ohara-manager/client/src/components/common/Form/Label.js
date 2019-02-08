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
import ReactTooltip from 'react-tooltip';
import uuid from 'uuid/v4';

const LabelWrapper = styled.label`
  position: relative;
  color: ${props => props.theme.lightBlue};
  width: ${({ width }) => width};
  font-size: 13px;
  margin: ${({ margin }) => margin};
`;

LabelWrapper.displayName = 'Label';

const TooltipIcon = styled.span`
  position: absolute;
  right: -18px;
  top: -2px;
`;

const Label = ({
  children,
  css = { margin: '0 0 8px', width: 'auto' },
  tooltipId = uuid(),
  tooltipString,
  tooltipRender,
  ...rest
}) => {
  return (
    <LabelWrapper {...css} {...rest}>
      {children}
      {tooltipString && (
        <TooltipIcon data-tip={tooltipString}>
          <i className="fas fa-info-circle" />
        </TooltipIcon>
      )}
      {tooltipString && <ReactTooltip />}
      {tooltipRender && (
        <TooltipIcon data-tip data-for={tooltipId}>
          <i className="fas fa-info-circle" />
        </TooltipIcon>
      )}
      {tooltipRender && (
        <ReactTooltip id={tooltipId}>{tooltipRender}</ReactTooltip>
      )}
    </LabelWrapper>
  );
};

Label.propTypes = {
  children: PropTypes.any,
  css: PropTypes.object,
  tooltipId: PropTypes.string,
  tooltipString: PropTypes.string,
  tooltipRender: PropTypes.any,
};

export default Label;
