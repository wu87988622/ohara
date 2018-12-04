import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const FormGroupWrapper = styled.div`
  display: flex;
  flex-direction: ${({ isInline }) => (isInline ? 'row' : 'column')};
  align-items: ${({ isInline }) => (isInline ? 'center' : 'flex-start')};
  width: ${({ width }) => width};
  height: ${({ height }) => height};
  margin: ${({ margin }) => margin};
`;

FormGroupWrapper.displayName = 'FormGroup';

const FormGroup = ({
  children,
  css = {
    width: 'auto',
    height: 'auto',
    margin: '0 0 20px',
  },
  isInline = false,
  ...rest
}) => {
  return (
    <FormGroupWrapper isInline={isInline} {...css} {...rest}>
      {children}
    </FormGroupWrapper>
  );
};

FormGroup.propTypes = {
  children: PropTypes.any.isRequired,
  css: PropTypes.object,
  isInline: PropTypes.bool,
};

export default FormGroup;
