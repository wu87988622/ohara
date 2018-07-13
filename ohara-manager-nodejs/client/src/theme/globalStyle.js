import { injectGlobal } from 'styled-components';

import '../../node_modules/toastr/build/toastr.css';

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
