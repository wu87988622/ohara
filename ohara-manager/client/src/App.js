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
import PropTypes from 'prop-types';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';

import GlobalStyle from 'theme/globalStyle';
import Theme from 'components/Theme';
import AppBar from 'components/Layout/AppBar';
import NotFoundPage from 'components/NotFoundPage';
import { useSnackbar } from './context/SnackbarContext';
import { Button } from 'components/common/Form';

const Container = styled.div`
  display: flex;

  /* 
    Make AppBar can stretch to the bottom of the brower view even if the content
    height is not enough to do so
  */
  min-height: 100vh;

  /* Resolution under 1024px is not supported, display a scrollbar */
  min-width: 1024px;
`;

const Main = styled.main`
  padding: ${props => props.theme.spacing(2)}px;
`;

const Workspace = props => {
  const { params } = props.match;
  const showMessage = useSnackbar();

  return params.workspaceName ? (
    <>
      <h1>Workerspace name: {params.workspaceName}</h1>
      <Button onClick={() => showMessage(params.workspaceName)}>
        Show me the name
      </Button>
    </>
  ) : (
    <h1>You don't have any workspace yet!</h1>
  );
};

Workspace.propTypes = {
  match: PropTypes.shape({
    params: PropTypes.shape({
      workspaceName: PropTypes.string,
    }).isRequired,
  }).isRequired,
};

const App = () => {
  return (
    <Router>
      <Container className="container">
        <GlobalStyle />
        <AppBar />
        <Main>
          <Switch>
            <Route exact path="/:workspaceName?" component={Workspace} />
            <Route exact path="/temp/theme" component={Theme} />
            <Route path="*" component={NotFoundPage} />
          </Switch>
        </Main>
      </Container>
    </Router>
  );
};

export default App;
