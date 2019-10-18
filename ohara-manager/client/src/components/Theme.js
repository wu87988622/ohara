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
import styled from 'styled-components';
import Typography from '@material-ui/core/Typography';

import Button from 'components/common/Form/Button';

const Colors = styled.div`
  .strap {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 50px;
    font-size: 14px;
    text-transform: uppercase;
    color: white;

    /* Logging out all theme props */
    padding: ${props => console.log(props.theme) /*eslint-disable-line */};
  }

  .primary {
    background: ${props => props.theme.palette.primary.main};
  }

  .primary-50 {
    background: ${props => props.theme.palette.primary[50]};
  }
  .primary-100 {
    background: ${props => props.theme.palette.primary[100]};
  }
  .primary-200 {
    background: ${props => props.theme.palette.primary[200]};
  }
  .primary-300 {
    background: ${props => props.theme.palette.primary[300]};
  }
  .primary-400 {
    background: ${props => props.theme.palette.primary[400]};
  }
  .primary-500 {
    background: ${props => props.theme.palette.primary[500]};
  }
  .primary-600 {
    background: ${props => props.theme.palette.primary[600]};
  }
  .primary-700 {
    background: ${props => props.theme.palette.primary[700]};
  }
  .primary-800 {
    background: ${props => props.theme.palette.primary[800]};
  }
  .primary-900 {
    background: ${props => props.theme.palette.primary[900]};
  }

  .success {
    background: ${props => props.theme.palette.success.main};
  }
  .info {
    background: ${props => props.theme.palette.info.main};
  }
  .warning {
    background: ${props => props.theme.palette.warning.main};
  }
  .error {
    background: ${props => props.theme.palette.error.main};
  }
`;

const Type = styled.div`
  padding: 50px;
`;

const ThemeWithStyledComponent = styled.div`
  display: inline-block;
  font-family: ${props => props.theme.typography.button.fontFamily};
  font-weight: ${props => props.theme.typography.button.fontWeight};
  letter-spacing: ${props => props.theme.typography.button.letterSpacing};
  line-height: ${props => props.theme.typography.button.lineHeight};
  text-transform: ${props => props.theme.typography.button.textTransform};
  background-color: ${props => props.theme.palette.background.default};
  padding: ${props => `${props.theme.spacing(1)}px`};
`;

const StyledButton = styled(Button)`
  background: linear-gradient(45deg, #fe6b8b 30%, #ff8e53 90%);
  border-radius: 3px;
  font-size: 16px;
  border: 0;
  color: white;
  height: 48px;
  padding: 0 30px;
  box-shadow: 0 3px 5px 2px rgba(255, 105, 135, 0.3);
`;

const TypographyTheme = () => {
  return (
    <>
      <ThemeWithStyledComponent>
        This div's text looks like that of a button.
      </ThemeWithStyledComponent>
      <StyledButton>Styled component!</StyledButton>
    </>
  );
};

const Theme = () => {
  return (
    <>
      <Colors>
        <div className="strap primary">#329BDD</div>
        <div className="strap primary-100">#B6E5FA</div>
        <div className="strap primary-200">#87D4F6</div>
        <div className="strap primary-300">#5DC3F2</div>
        <div className="strap primary-400">#44B6EF</div>
        <div className="strap primary-500">#36A9EC</div>
        <div className="strap primary-600">#329BDD</div>
        <div className="strap primary-700">#2D88C9</div>
        <div className="strap primary-800">#2977B5</div>
        <div className="strap primary-900">#215792</div>

        <div className="strap success">#00D68F</div>
        <div className="strap info">#329BDD</div>
        <div className="strap warning">#FFAA00</div>
        <div className="strap error">#FF3D71</div>
      </Colors>
      <Type>
        <Typography variant="h1" component="h2" gutterBottom>
          h1. Heading
        </Typography>
        <Typography variant="h2" gutterBottom>
          h2. Heading
        </Typography>
        <Typography variant="h3" gutterBottom>
          h3. Heading
        </Typography>
        <Typography variant="h4" gutterBottom>
          h4. Heading
        </Typography>
        <Typography variant="h5" gutterBottom>
          h5. Heading
        </Typography>
        <Typography variant="h6" gutterBottom>
          h6. Heading
        </Typography>
        <Typography variant="subtitle1" gutterBottom>
          subtitle1. Lorem ipsum dolor sit amet, consectetur adipisicing elit.
          Quos blanditiis tenetur
        </Typography>
        <Typography variant="subtitle2" gutterBottom>
          subtitle2. Lorem ipsum dolor sit amet, consectetur adipisicing elit.
          Quos blanditiis tenetur
        </Typography>
        <Typography variant="body1" gutterBottom>
          body1. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Quos
          blanditiis tenetur unde suscipit, quam beatae rerum inventore
          consectetur, neque doloribus, cupiditate numquam dignissimos laborum
          fugiat deleniti? Eum quasi quidem quibusdam.
        </Typography>
        <Typography variant="body2" gutterBottom>
          body2. Lorem ipsum dolor sit amet, consectetur adipisicing elit. Quos
          blanditiis tenetur unde suscipit, quam beatae rerum inventore
          consectetur, neque doloribus, cupiditate numquam dignissimos laborum
          fugiat deleniti? Eum quasi quidem quibusdam.
        </Typography>
        <Typography variant="button" display="block" gutterBottom>
          button text
        </Typography>
        <Typography variant="caption" display="block" gutterBottom>
          caption text
        </Typography>
        <Typography variant="overline" display="block" gutterBottom>
          overline text
        </Typography>

        <TypographyTheme />
      </Type>
    </>
  );
};

export default Theme;
