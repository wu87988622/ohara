/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  blue,
  blueHover,
  darkerBlue,
  lightBlue,
  lighterGray,
  lightestBlue,
  red,
  redHover,
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

export const dangerBtn = {
  color: white,
  bgColor: red,
  border: 0,
  bgHover: redHover,
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
