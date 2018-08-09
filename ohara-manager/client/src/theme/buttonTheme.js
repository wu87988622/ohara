import { lighterGray, darkerBlue, blue, blueHover, white } from './variables';

export const submitButton = {
  color: white,
  bgColor: blue,
  border: 0,
  bgHover: blueHover,
  colorHover: white,
  borderHover: 0,
};

export const cancelButton = {
  color: darkerBlue,
  bgColor: white,
  border: 0,
  bgHover: lighterGray,
  colorHover: darkerBlue,
  borderHover: 0,
};

export const defaultButton = {
  color: darkerBlue,
  bgColor: white,
  border: `1px solid ${lighterGray}`,
  bgHover: blueHover,
  colorHover: white,
  borderHover: `1px solid ${blueHover}`,
};
