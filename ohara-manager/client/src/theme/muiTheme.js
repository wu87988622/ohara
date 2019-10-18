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

import { createMuiTheme } from '@material-ui/core/styles';

const MuiTheme = createMuiTheme({
  palette: {
    primary: {
      50: '#E2F5FD',
      100: '#B6E5FA',
      200: '#87D4F6',
      300: '#5DC3F2',
      400: '#44B6EF',
      500: '#36A9EC',
      600: '#329BDD',
      700: '#2D88C9',
      800: '#2977B5',
      900: '#215792',
      main: '#329BDD',
      light: '#72CCFF',
      dark: '#006DAB',
      contrastText: '#FFFFFF',
    },
    success: {
      main: '#00D68F',
      light: '#61FFC0',
      dark: '#00A361',
      contrastText: '#FFFFFF',
    },
    info: {
      main: '#329BDD',
      light: '#72CCFF',
      dark: '#006DAB',
      contrastText: '#FFFFFF',
    },
    warning: {
      main: '#FFAA00',
      light: '#FFDC4A',
      dark: '#C67B00',
      contrastText: '#000000',
    },
    error: {
      main: '#FF3D71',
      light: '#FF769F',
      dark: '#C60046',
      contrastText: '#000000',
    },
  },
});

export default MuiTheme;
