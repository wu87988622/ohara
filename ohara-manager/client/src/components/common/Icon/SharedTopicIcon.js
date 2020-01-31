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

import * as generate from 'utils/generate';

export const SharedTopicIcon = ({
  width,
  height,
  fillColor = 'currentColor',
  statusColor,
  ...otherProps
}) => {
  const maskId = generate.id();

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
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
        <g transform="translate(-304.000000, -2906.000000)">
          <g transform="translate(305.000000, 2907.000000)">
            <g>
              <g>
                <path
                  strokeOpacity="0.54"
                  stroke="#000000"
                  transform="translate(28.000000, 28.000000) scale(-1, 1) translate(-28.000000, -28.000000) "
                  d="M28,56 C43.463973,56 56,43.463973 56,28 C56,12.536027 43.463973,0 28,0 C12.536027,0 0,12.536027 0,28"
                ></path>
              </g>
              <g transform="translate(19.000000, 21.000000)"></g>
              <g transform="translate(32.000000, 32.000000)">
                <mask id={maskId} fill="white">
                  <path d="M18,16.08 C17.24,16.08 16.56,16.38 16.04,16.85 L8.91,12.7 C8.96,12.47 9,12.24 9,12 C9,11.76 8.96,11.53 8.91,11.3 L15.96,7.19 C16.5,7.69 17.21,8 18,8 C19.66,8 21,6.66 21,5 C21,3.34 19.66,2 18,2 C16.34,2 15,3.34 15,5 C15,5.24 15.04,5.47 15.09,5.7 L8.04,9.81 C7.5,9.31 6.79,9 6,9 C4.34,9 3,10.34 3,12 C3,13.66 4.34,15 6,15 C6.79,15 7.5,14.69 8.04,14.19 L15.16,18.35 C15.11,18.56 15.08,18.78 15.08,19 C15.08,20.61 16.39,21.92 18,21.92 C19.61,21.92 20.92,20.61 20.92,19 C20.92,17.39 19.61,16.08 18,16.08 Z"></path>
                </mask>
                <g fillRule="nonzero"></g>
                <g
                  mask={`url(#${maskId})`}
                  fill="#000000"
                  fillOpacity="0.54"
                  fillRule="evenodd"
                >
                  <rect x="0" y="0" width="24" height="24"></rect>
                </g>
              </g>
            </g>
          </g>
        </g>
      </g>
      <g
        className="topic-status"
        fill={statusColor}
        transform="translate(20, 20) scale(0.8)"
      >
        <path d="M0 0h24v24H0z" fill="none" />
        <path d="M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z" />
      </g>
    </svg>
  );
};

SharedTopicIcon.propTypes = {
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  statusColor: PropTypes.string.isRequired,
  fillColor: PropTypes.string,
};
