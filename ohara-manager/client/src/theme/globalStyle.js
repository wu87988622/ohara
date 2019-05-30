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

import ReactModal from 'react-modal';
import { injectGlobal } from 'styled-components';

import {
  blue,
  blueHover,
  red,
  orange,
  green,
  lightGray,
  whiteSmoke,
  darkerBlue,
  durationNormal,
} from './variables';

import '../../node_modules/toastr/build/toastr.css';

// Global styles for ReactModal
// TODO: use styled-component to style ReactModal
ReactModal.defaultStyles.overlay.backgroundColor = 'rgba(32, 42, 65, .9)';
ReactModal.defaultStyles.overlay.zIndex = 1100;

export default injectGlobal`
  @import url('https://fonts.googleapis.com/css?family=Merriweather+Sans:400,700|Roboto:400,700,900');
  
  *, *:before, *:after {
    box-sizing: border-box;
  }

  ::placeholder {
    color: ${lightGray};
  }

  body {
    color: ${darkerBlue};
    padding: 0;
    margin: 0;
    font-family: Roboto, sans-serif;
    background-color: ${whiteSmoke};

    /* Disable vertical scroll bar when there's an active modal */
    &.ReactModal__Body--open {
      overflow-y: hidden;
    }
  }

  a {
    transition: ${durationNormal} all;
    text-decoration: none;
    color: ${blue};
    
    &:hover {
      transition: ${durationNormal} all;
      color: ${blueHover}
    }
  }

  ul, li {
    margin: 0;
    padding: 0;
    list-style: none;
  }

  button, input {
    outline: none;
  }

  button {
    cursor: pointer;
  }

  /* toastr */
  #toast-container > div.toast {
    opacity: 0.98;
    padding: 20px 25px;
    box-shadow: none;
    width: auto;
    max-width: 300px;
    background-image: none !important;
    box-shadow: 2px 3px 27px rgba(0, 0, 0, .2);

    &.toast-success {
        background-color: ${green}
    }

    &.toast-info {
      background-color: ${blue};
    }

    &.toast-warning {
      background-color: ${orange};
    }

    &.toast-error {
      background-color: ${red};
    }

    .toast-title, toast-message {
      font-size: 14px;
      font-weight: normal;
      word-break: break-word;
      line-height: 1.6;
    }

    &:hover {
      box-shadow: 2px 3px 27px rgba(0, 0, 0, .2);
    }
  }
`;
