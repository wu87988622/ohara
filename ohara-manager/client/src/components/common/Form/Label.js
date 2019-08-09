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

const LabelWrapper = styled.label`
  color: ${props => props.theme.lightBlue};
  width: ${props => props.width};
  font-size: 13px;
  margin: ${props => props.margin};
  display: block;

  .label-name {
    margin-right: 4px;
  }
`;

LabelWrapper.displayName = 'Label';

const TooltipIcon = styled.span`
  float: ${props => props.alignment};
`;

const Label = ({
  children,
  css = { margin: '0 0 8px', width: 'auto' },
  tooltipString,
  tooltipRender,
  tooltipAlignment = 'left',
  required = false,
  ...rest
}) => {
  return (
    <LabelWrapper {...css} {...rest}>
      <span className="label-name">{children}</span>{' '}
      {required && <span>*</span>}
      {tooltipString && (
        <TooltipIcon alignment={tooltipAlignment} data-tip={tooltipString}>
          <i className="fas fa-info-circle" />
        </TooltipIcon>
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
  required: PropTypes.bool,
  tooltipAlignment: PropTypes.string,
};

export default Label;
