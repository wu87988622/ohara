import {
  blue,
  blueHover,
  darkerBlue,
  lightBlue,
  lighterGray,
  lightestBlue,
  red,
  white,
  dimBlue,
  darkBlue,
} from './variables';

export const primaryBtn = {
  color: white,
  bgColor: blue,
  border: 0,
  bgHover: blueHover,
  colorHover: white,
  borderHover: 0,
};

export const deleteBtn = {
  color: lightBlue,
  bgColor: 'transparent',
  border: `1px solid ${lightestBlue}`,
  bgHover: red,
  colorHover: white,
  borderHover: `1px solid ${red}`,
};

export const cancelBtn = {
  color: dimBlue,
  bgColor: white,
  border: 0,
  bgHover: lighterGray,
  colorHover: darkBlue,
  borderHover: 0,
};

export const defaultBtn = {
  color: darkerBlue,
  bgColor: white,
  border: `1px solid ${lighterGray}`,
  bgHover: blueHover,
  colorHover: white,
  borderHover: `1px solid ${blueHover}`,
};
