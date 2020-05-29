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

export const PipelineOnlyTopicIcon = ({
  width,
  height,
  fillColor = 'currentColor',
  statusColor,
  ...otherProps
}) => {
  return (
    <svg
      {...otherProps}
      fill={fillColor}
      height={height}
      version="1.1"
      viewBox="0 0 58 58"
      width={width}
      xmlns="http://www.w3.org/2000/svg"
      xmlnsXlink="http://www.w3.org/1999/xlink"
    >
      <g fill="none" fillRule="evenodd" stroke="none" strokeWidth="1">
        <g transform="translate(-389.000000, -2906.000000)">
          <g transform="translate(390.000000, 2907.000000)">
            <circle
              cx="28"
              cy="28"
              r="28"
              stroke="#000000"
              strokeOpacity="0.54"
            ></circle>
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

PipelineOnlyTopicIcon.propTypes = {
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  statusColor: PropTypes.string.isRequired,
  fillColor: PropTypes.string,
};
