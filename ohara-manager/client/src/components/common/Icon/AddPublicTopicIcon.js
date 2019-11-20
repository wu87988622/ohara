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

export const AddPublicTopicIcon = ({
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
      fill={fillColor}
      xmlns="http://www.w3.org/2000/svg"
      xmlnsXlink="http://www.w3.org/1999/xlink"
      viewBox="0 0 40 40"
    >
      <defs>
        <path
          d="M0 32h40v-8H0v8zm4-6h4v4H4v-4zM0 0v8h40V0H0zm8 6H4V2h4v4zM0 20h40v-8H0v8zm4-6h4v4H4v-4z"
          id="a"
        />
        <path
          d="M16 0C7.168 0 0 7.168 0 16c0 4.788 2.107 9.067 5.443 12h21.12c.509-.448.989-.921 1.437-1.43V16 5.4A16.003 16.003 0 0016 0z"
          id="c"
        />
        <path
          d="M19.5 17.42c-.823 0-1.56.325-2.123.834l-7.724-4.496c.054-.249.097-.498.097-.758s-.043-.51-.098-.758l7.638-4.453a3.236 3.236 0 002.21.878 3.246 3.246 0 003.25-3.25 3.246 3.246 0 00-3.25-3.25 3.246 3.246 0 00-3.25 3.25c0 .26.043.509.098.758L8.71 10.627A3.236 3.236 0 006.5 9.75 3.246 3.246 0 003.25 13a3.246 3.246 0 003.25 3.25c.856 0 1.625-.336 2.21-.877l7.713 4.506a3.056 3.056 0 00-.086.704 3.167 3.167 0 003.163 3.164 3.167 3.167 0 003.163-3.164A3.167 3.167 0 0019.5 17.42z"
          id="e"
        />
      </defs>
      <g fillRule="evenodd">
        <g>
          <mask id="b" fill="#fff">
            <use xlinkHref="#a" />
          </mask>
          <g mask="url(#b)" fill="#000">
            <path d="M0-6h48v48H0z" />
          </g>
        </g>
        <g transform="translate(12 12)">
          <mask id="d" fill="#fff">
            <use xlinkHref="#c" />
          </mask>
          <g mask="url(#d)" fill="#FFF">
            <path d="M0 0h32v32H0z" />
          </g>
        </g>
        <g transform="translate(12 14)">
          <mask id="f" fill="#fff">
            <use xlinkHref="#e" />
          </mask>
          <g mask="url(#f)" fill="#000">
            <path d="M0 0h26v26H0z" />
          </g>
        </g>
      </g>
    </svg>
  );
};

AddPublicTopicIcon.propTypes = {
  width: PropTypes.number.isRequired,
  height: PropTypes.number.isRequired,
  fillColor: PropTypes.string,
};
