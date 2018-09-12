import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import * as _ from '../../../utils/helpers';

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

const Select = ({
  list,
  selected,
  handleChange,
  isObject = false,
  width = '100%',
  ...rest
}) => {
  if (_.isEmpty(list)) return null;

  const _selected = isObject ? selected.name : selected;

  return (
    <SelectWrapper
      value={_selected}
      onChange={handleChange}
      width={width}
      {...rest}
    >
      {isObject
        ? list.map(({ uuid, name }, idx) => {
            return (
              <option key={idx} data-uuid={uuid}>
                {name}
              </option>
            );
          })
        : list.map((name, idx) => {
            return <option key={idx}>{name}</option>;
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
