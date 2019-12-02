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

import React from 'react';
import PropTypes from 'prop-types';

export const PrivateTopicIcon = ({
  width,
  height,
  fillColor = 'currentColor',
  ...otherProps
}) => {
  return (
    <svg
      {...otherProps}
      width={width}
      height={height}
      viewBox="0 0 58 58"
      fill={fillColor}
      version="1.1"
      xmlns="http://www.w3.org/2000/svg"
      xmlnsXlink="http://www.w3.org/1999/xlink"
    >
      <defs>
        <path
          d="M0.666666667,13.6666667 L17.3333333,13.6666667 L17.3333333,10.3333333 L0.666666667,10.3333333 L0.666666667,13.6666667 Z M2.33333333,11.1666667 L4,11.1666667 L4,12.8333333 L2.33333333,12.8333333 L2.33333333,11.1666667 Z M0.666666667,0.333333333 L0.666666667,3.66666667 L17.3333333,3.66666667 L17.3333333,0.333333333 L0.666666667,0.333333333 Z M4,2.83333333 L2.33333333,2.83333333 L2.33333333,1.16666667 L4,1.16666667 L4,2.83333333 Z M0.666666667,8.66666667 L17.3333333,8.66666667 L17.3333333,5.33333333 L0.666666667,5.33333333 L0.666666667,8.66666667 Z M2.33333333,6.16666667 L4,6.16666667 L4,7.83333333 L2.33333333,7.83333333 L2.33333333,6.16666667 Z"
          id="privateTopic-1"
        ></path>
      </defs>
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
        <g transform="translate(-389.000000, -2906.000000)">
          <g transform="translate(390.000000, 2907.000000)">
            <g>
              <circle
                strokeOpacity="0.54"
                stroke="#000000"
                cx="28"
                cy="28"
                r="28"
              ></circle>
              <g transform="translate(19.000000, 21.000000)">
                <mask id="d-2" fill="white">
                  <use xlinkHref="#privateTopic-1"></use>
                </mask>
                <g fillRule="nonzero"></g>
                <g mask="url(#d-2)" fill="#000000" fillOpacity="0.54">
                  <g transform="translate(-1.000000, -3.000000)">
                    <rect x="0" y="0" width="20" height="20"></rect>
                  </g>
                </g>
              </g>
            </g>
          </g>
        </g>
      </g>
    </svg>
  );
};

PrivateTopicIcon.propTypes = {
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  fillColor: PropTypes.string,
};
