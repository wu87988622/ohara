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
      viewBox="0 0 45 40"
      fill={fillColor}
      version="1.1"
      xmlns="http://www.w3.org/2000/svg"
      xmlnsXlink="http://www.w3.org/1999/xlink"
    >
      <defs>
        <path
          d="M0,32 L16.5260369,32 C16.5066809,30.6622017 16.6097623,29.3288684 16.8352812,28 C17.0608001,26.6711316 17.4102719,25.3377983 17.8836966,24 L0,24 L0,32 Z M4,26 L8,26 L8,30 L4,30 L4,26 Z M0,0 L0,8 L40,8 L40,0 L0,0 Z M8,6 L4,6 L4,2 L8,2 L8,6 Z M0,20 L18.8635283,20 L19.7320847,20 C20.7906472,18.2132773 21.8837588,16.7187326 23.0114194,15.5163659 C24.1390801,14.3139993 25.5469486,13.1418773 27.235025,12 L24.3201814,12 L0,12 L0,20 Z M27.6718937,28.9767888 C26.9635962,29.6129616 26.0269969,30 25,30 C22.790861,30 21,28.209139 21,26 C21,23.790861 22.790861,22 25,22 C26.1659801,22 27.21544,22.4988823 27.9465696,23.2948367 L37.1149657,17.9560221 C37.03984,17.6496895 37,17.3294973 37,17 C37,14.790861 38.790861,13 41,13 C43.209139,13 45,14.790861 45,17 C45,19.209139 43.209139,21 41,21 C39.9299346,21 38.9580076,20.5798189 38.2402106,19.8954482 L28.9459582,25.2942693 L28.9333766,25.2690979 C28.977132,25.5060705 29,25.7503647 29,26 C29,26.3748154 28.9484474,26.7375901 28.852046,27.0816202 L28.866043,27.0591428 L38.3451596,33.0079818 C39.0514043,32.3808668 39.9812241,32 41,32 C43.209139,32 45,33.790861 45,36 C45,38.209139 43.209139,40 41,40 C38.790861,40 37,38.209139 37,36 C37,35.6333471 37.0493317,35.2782162 37.1417198,34.9408826 L27.6668284,28.9849231 L27.6718937,28.9767888 Z M4,14 L8,14 L8,18 L4,18 L4,14 Z"
          id="path-1"
        ></path>
      </defs>
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
        <g transform="translate(-126.000000, -3069.000000)">
          <g transform="translate(126.000000, 3069.000000)">
            <mask id="mask-2" fill="white">
              <use xlinkHref="#path-1"></use>
            </mask>
            <g fillRule="nonzero"></g>
            <rect
              fill="#000000"
              mask="url(#mask-2)"
              x="-2"
              y="0"
              width="48"
              height="48"
            ></rect>
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
