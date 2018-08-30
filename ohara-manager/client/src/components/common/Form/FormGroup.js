import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const FormGroupWrapper = styled.div`
  display: flex;
  flex-direction: column;
  margin-bottom: 30px;
`;

FormGroupWrapper.displayName = 'FormGroup';

const FormGroup = ({ children, ...rest }) => {
  return <FormGroupWrapper {...rest}>{children}</FormGroupWrapper>;
};

FormGroup.propTypes = {
  children: PropTypes.array,
};

export default FormGroup;
