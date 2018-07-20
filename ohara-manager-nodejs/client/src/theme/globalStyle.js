import { injectGlobal } from 'styled-components';
import ReactModal from 'react-modal';

import '../../node_modules/toastr/build/toastr.css';

// Global styles for ReactModal
// TODO: use styled-component to style ReactModal
ReactModal.defaultStyles.overlay.backgroundColor = 'rgba(0, 0, 0, .5)';
ReactModal.defaultStyles.overlay.zIndex = 1100;

export default injectGlobal`
  @import url('https://fonts.googleapis.com/css?family=Montserrat:400,900|Roboto');

  body {
    padding: 0;
    margin: 0;
    font-family: Roboto, sans-serif;
    background-color: #f5f5f5;
  }

   h1 {
    font-family: Montserrat;
  }

`;
