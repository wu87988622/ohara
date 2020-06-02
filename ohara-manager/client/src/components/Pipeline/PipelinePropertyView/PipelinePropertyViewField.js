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
import InfoIcon from '@material-ui/icons/Info';
import NumberFormat from 'react-number-format';

import { Tooltip } from 'components/common/Tooltip';
import { FieldWrapper } from './PipelinePropertyViewFieldStyles';

const PipelinePropertyViewField = (props) => {
  const {
    label,
    value,
    slot = null,
    documentation = '',
    isPort,
    ...rest
  } = props;

  const hasDocs = documentation.length > 0;

  return (
    <FieldWrapper {...rest}>
      <Field isPort={isPort} label={label} value={value} />
      {slot}

      {hasDocs && (
        <div className="docs-tooltip">
          <Tooltip interactive placement="left" title={documentation}>
            <InfoIcon className="docs-icon" />
          </Tooltip>
        </div>
      )}
    </FieldWrapper>
  );
};

PipelinePropertyViewField.propTypes = {
  label: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.object,
  ]).isRequired,
  slot: PropTypes.node,
  documentation: PropTypes.string,
  isPort: PropTypes.bool,
};

function Field({ label, value, isPort }) {
  return (
    <div className="field">
      <Typography className="field-label" component="span" variant="body2">
        {label}
      </Typography>
      <Typography className="field-value" component="span" variant="body2">
        {isPort || isNaN(Number(value)) ? (
          value
        ) : (
          <NumberFormat
            displayType="text"
            thousandSeparator
            value={Number(value)}
          />
        )}
      </Typography>
    </div>
  );
}

Field.propTypes = {
  label: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([
    PropTypes.string,
    PropTypes.number,
    PropTypes.object,
  ]).isRequired,
  isPort: PropTypes.bool,
};

export default PipelinePropertyViewField;
