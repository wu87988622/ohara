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
import PropTypes from 'prop-types';
import Typography from '@material-ui/core/Typography';

import GlobalStyle from 'theme/globalStyle';
import { Button } from 'components/common/Form';
import { ReactComponent as ErrorBoundarySvg } from 'images/error-boundary.svg';
import { Wrapper } from './ErrorPageStyles';

// Error boundary is not supported in React Hooks, we need
// to use our good old class component to handle App error
class ErrorBoundary extends React.Component {
  state = { hasError: false };

  static getDerivedStateFromError() {
    // Update state so the next render will show the fallback UI.
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      // Fallback UI
      return (
        <>
          {/* Global style is somehow missing in this component, need to manually declare it again here */}
          <GlobalStyle />
          <Wrapper>
            <Typography variant="h1">
              500: Oops, something went terribly wrong!
            </Typography>
            <Typography variant="body1">
              You either tried some shady route or you came here by mistake.
              Whichever it is, try using the navigation
            </Typography>

            <ErrorBoundarySvg width="680" />

            <Button
              variant="outlined"
              color="primary"
              onClick={() => (window.location.href = '/')}
            >
              BACK TO HOME
            </Button>
          </Wrapper>
        </>
      );
    }

    return this.props.children;
  }
}

ErrorBoundary.propTypes = {
  children: PropTypes.any.isRequired,
};

export default ErrorBoundary;
