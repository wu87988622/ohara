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
    padding: 20px 20px 20px 20px;
    box-shadow: none;
    width: auto;
    max-width: 300px;
    background-image: none !important;

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
      font-size: 12px;
      font-weight: normal;
    }

    &:hover {
      box-shadow: none;
    }
  }
`;
