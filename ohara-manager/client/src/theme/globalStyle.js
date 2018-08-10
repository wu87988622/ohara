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
  @import url('https://fonts.googleapis.com/css?family=Merriweather+Sans:400,700|Roboto');
  
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

  #toast-container > div.toast {
    opacity: 0.9;
    padding: 20px 20px 20px 35px;
    box-shadow: none;
    width: auto;
    background-image: none !important;

    &.toast-success {
        background-color: ${green}

      &:before {
        font-family: "Font Awesome 5 Free";
        content: "\f058";
        position: absolute;
        left: 15px;
      }
    }

    &.toast-info {
      background-color: ${blue};
      &:before {
        font-family: "Font Awesome 5 Free";
        content: "\f059";
        position: absolute;
        left: 15px;
      }
    }

    &.toast-warning {
      background-color: ${orange};
      &:before {
        font-family: "Font Awesome 5 Free";
        content: "\f059";
        position: absolute;
        left: 15px;
      }
    }

    &.toast-error {
      background-color: ${red};
      &:before {
        font-family: "Font Awesome 5 Free";
        content: "\f057";
        position: absolute;
        left: 15px;
      }
    }

    .toast-title {
      font-size: 13px;
      font-weight: normal;
    }

    &:hover {
      box-shadow: none;
    }
  }
`;
