import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const FormGroupWrapper = styled.div`
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  width: ${({ width }) => width};
  height: ${({ height }) => height};
  margin: ${({ margin }) => margin};
`;

FormGroupWrapper.displayName = 'FormGroup';

const FormGroup = ({
  children,
  width = 'auto',
  height = 'auto',
  margin = '0 0 20px 0',
  ...rest
}) => {
  return (
    <FormGroupWrapper width={width} height={height} margin={margin} {...rest}>
      {children}
    </FormGroupWrapper>
  );
};

FormGroup.propTypes = {
  children: PropTypes.any,
  width: PropTypes.string,
  height: PropTypes.string,
  margin: PropTypes.string,
};

export default FormGroup;
