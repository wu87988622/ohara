import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import {
  white,
  blue,
  lightBlue,
  lighterGray,
  radiusNormal,
  durationNormal,
} from '../../../theme/variables';

const SelectWrapper = styled.select`
  font-size: 13px;
  font-family: inherit;
  color: ${lightBlue};
  border: 1px solid ${lighterGray};
  padding: 10px 10px 10px 15px;
  border-radius: ${radiusNormal};
  background-color: ${white};
  width: ${({ width }) => width};
  height: 40px;
  outline: 0;

  &:focus {
    border-color: ${blue};
    box-shadow: 0 0 0 3px rgba(76, 132, 255, 0.25);
    transition: ${durationNormal} all;
  }
`;

SelectWrapper.displayName = 'Select';

const Select = ({ list, selected, handleChange, width = '100%', ...rest }) => {
  return (
    <SelectWrapper
      value={selected.name}
      onChange={handleChange}
      width={width}
      {...rest}
    >
      {list.map(({ uuid, name }) => {
        return (
          <option key={uuid} data-uuid={uuid}>
            {name}
          </option>
        );
      })}
    </SelectWrapper>
  );
};

Select.propTypes = {
  list: PropTypes.array.isRequired,
  selected: PropTypes.oneOfType([PropTypes.string, PropTypes.object])
    .isRequired,
  handleChange: PropTypes.func.isRequired,
};

export default Select;
