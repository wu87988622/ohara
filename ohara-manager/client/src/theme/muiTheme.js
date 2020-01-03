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
  overrides: {
    MuiFilledInput: {
      input: {
        paddingRight: 0,
        paddingLeft: 0,
      },
    },
    MuiInputLabel: {
      filled: {
        transform: 'translate(0, 30px) scale(1)',
        '&$shrink': {
          transform: 'translate(0, 4px) scale(1)',
        },
      },
      formControl: {
        transform: 'translate(0, 30px) scale(1)',
      },
    },
    MuiFormHelperText: {
      contained: {
        marginLeft: 0,
      },
    },
  },
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
      main: '#ffc107',
      light: '#ffd54f',
      dark: '#ffa000',
      contrastText: '#000000',
    },
    error: {
      main: '#FF3D71',
      light: '#FF769F',
      dark: '#C60046',
      contrastText: '#000000',
    },
  },
  typography: {
    h1: {
      fontWeight: 500,
      fontSize: '35px',
      lineHeight: '40px',
      letterSpacing: '-0.24px',
    },
    h2: {
      fontWeight: 500,
      fontSize: '29px',
      lineHeight: '32px',
      letterSpacing: '-0.24px',
    },
    h3: {
      fontWeight: 500,
      fontSize: '24px',
      lineHeight: '28px',
      letterSpacing: '-0.06px',
    },
    h4: {
      fontWeight: 500,
      fontSize: '20px',
      lineHeight: '24px',
      letterSpacing: '-0.06px',
    },
    h5: {
      fontWeight: 500,
      fontSize: '16px',
      lineHeight: '20px',
      letterSpacing: '-0.05px',
    },
    h6: {
      fontWeight: 500,
      fontSize: '14px',
      lineHeight: '20px',
      letterSpacing: '-0.05px',
    },
    subtitle1: {
      fontWeight: 400,
      fontSize: '16px',
      lineHeight: '25px',
      letterSpacing: '-0.05px',
    },
    subtitle2: {
      fontWeight: 400,
      fontSize: '14px',
      lineHeight: '21px',
      letterSpacing: '-0.05px',
    },
    body1: {
      fontWeight: 400,
      fontSize: '14px',
      lineHeight: '21px',
      letterSpacing: '-0.05px',
    },
    body2: {
      fontWeight: 400,
      fontSize: '12px',
      lineHeight: '18px',
      letterSpacing: '-0.05px',
    },
    caption: {
      fontWeight: 400,
      fontSize: '11px',
      lineHeight: '13px',
      letterSpacing: '-0.05px',
    },
    overline: {
      fontWeight: 500,
      fontSize: '11px',
      lineHeight: '13px',
      letterSpacing: '0.33px',
      textTransform: 'uppercase',
    },
  },
  zIndex: {
    toolbox: 1210,
    flyingPaper: 1220,
  },
});

export default MuiTheme;
