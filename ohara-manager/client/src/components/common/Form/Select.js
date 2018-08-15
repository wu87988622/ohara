import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import {
  lightBlue,
  lighterGray,
  radiusNormal,
  white,
} from '../../../theme/variables';

const SelectWrapper = styled.select`
  font-size: 13px;
  font-family: inherit;
  color: ${lightBlue};
  border: 1px solid ${lighterGray};
  padding: 10px 10px 10px 15px;
  border-radius: ${radiusNormal};
  background-color: ${white};
  height: 40px;
  width: 100%;
  outline: 0;
`;

SelectWrapper.displayName = 'Select';

const Select = ({ list, selected, handleChange }) => {
  return (
    <SelectWrapper value={selected.name} onChange={handleChange}>
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
