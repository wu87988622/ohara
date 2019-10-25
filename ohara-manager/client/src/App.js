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

import React, { useEffect } from 'react';
import styled from 'styled-components';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import GlobalStyle from 'theme/globalStyle';
import Theme from 'components/Theme';
import AppBar from 'components/Layout/AppBar';
import NotFoundPage from 'components/NotFoundPage';
import { useSnackbar } from './context/SnackbarContext';

const Container = styled.div`
  display: flex;

  /* 
    Make AppBar can stretch to the bottom of the brower view even if the content
    height is not enough to do so
  */
  min-height: 100vh;
`;

const Main = styled.main`
  padding: ${props => props.theme.spacing(2)}px;
`;

const App = () => {
  const showMessage = useSnackbar();

  // Example usage, should be removed later
  useEffect(() => {
    showMessage('Hello world!');
  }, [showMessage]);

  return (
    <Router>
      <Container className="container">
        <GlobalStyle />
        <AppBar />
        <Main>
          <Switch>
            <Route
              exact
              path="/"
              component={() => (
                <h1 onClick={() => showMessage('Ah?')}>Hello ohara manager!</h1>
              )}
            />
            <Route exact path="/theme" component={Theme} />
            <Route path="*" component={NotFoundPage} />
          </Switch>
        </Main>
      </Container>
    </Router>
  );
};

export default App;
