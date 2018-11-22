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
} from 'theme/variables';

const SelectWrapper = styled.select`
  font-size: 13px;
  font-family: inherit;
  color: ${lightBlue};
  border: 1px solid ${lighterGray};
  padding: 10px 10px 10px 15px;
  border-radius: ${radiusNormal};
  background-color: ${white};
  width: ${({ width }) => width};
  height: ${({ height }) => height};
  outline: 0;

  &:focus {
    border-color: ${blue};
    box-shadow: 0 0 0 3px rgba(76, 132, 255, 0.25);
    transition: ${durationNormal} all;
  }
`;

SelectWrapper.displayName = 'Select';

const Select = ({
  list = [],
  selected = '',
  handleChange,
  isObject = false,
  height = '32px',
  width = '100%',
  ...rest
}) => {
  const _selected = isObject ? selected && selected.name : selected;

  return (
    <SelectWrapper
      value={_selected}
      onChange={handleChange}
      width={width}
      height={height}
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
  selected: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  handleChange: PropTypes.func.isRequired,
  isObject: PropTypes.bool,
  width: PropTypes.string,
  height: PropTypes.string,
  list: PropTypes.array,
};

export default Select;
