import React from 'react';
import PropTypes from 'prop-types';
import { Input } from 'common/Form';

const InputField = ({
  input: { name, onChange, value, ...restInput },
  meta,
  ...rest
}) => (
  <Input
    {...rest}
    name={name}
    helperText={meta.touched ? meta.error : undefined}
    error={meta.error && meta.touched}
    inputProps={restInput}
    handleChange={onChange}
    value={value}
  />
);

InputField.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.string.isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    touched: PropTypes.bool,
    error: PropTypes.object,
  }).isRequired,
};

export default InputField;
