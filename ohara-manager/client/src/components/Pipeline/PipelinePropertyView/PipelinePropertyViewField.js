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

const PipelinePropertyViewField = props => {
  const { label, value, slot = null, documentation = '', ...rest } = props;

  const hasDocs = documentation.length > 0;

  return (
    <FieldWrapper {...rest}>
      <Field label={label} value={value} />
      {slot}

      {hasDocs && (
        <div className="docs-tooltip">
          <Tooltip title={documentation} placement="left" interactive>
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
};

function Field({ label, value }) {
  return (
    <div className="field">
      <Typography variant="body2" className="field-label" component="span">
        {label}
      </Typography>
      <Typography variant="body2" className="field-value" component="span">
        {isNaN(Number(value)) ? (
          value
        ) : (
          <NumberFormat
            value={Number(value)}
            displayType="text"
            thousandSeparator
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
};

export default PipelinePropertyViewField;
