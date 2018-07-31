import ReactModal from 'react-modal';
import { injectGlobal } from 'styled-components';
import { primary, primaryHover, lighterGray, darkBlue } from './colors';

import '../../node_modules/toastr/build/toastr.css';

// Global styles for ReactModal
// TODO: use styled-component to style ReactModal
ReactModal.defaultStyles.overlay.backgroundColor = 'rgba(0, 0, 0, .5)';
ReactModal.defaultStyles.overlay.zIndex = 1100;

export default injectGlobal`
  @import url('https://fonts.googleapis.com/css?family=Merriweather+Sans:400,700|Roboto');
  
  *, *:before, *:after {
    box-sizing: border-box;
  }

  body {
    color: ${darkBlue};
    padding: 0;
    margin: 0;
    font-family: Roboto, sans-serif;
    background-color: ${lighterGray};
  }

   h1 {
    font-family: Merriweather;
  }

  a {
    transition: .3s all;
    text-decoration: none;
    color: ${primary};
    
    &:hover {
      transition: .3s all;
      color: ${primaryHover}
    }
  }

  ul, li {
    margin: 0;
    padding: 0;
    list-style: none;
  }

`;
