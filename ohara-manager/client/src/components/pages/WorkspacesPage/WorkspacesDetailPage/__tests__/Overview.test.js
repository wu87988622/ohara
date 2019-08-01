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
import { cleanup, waitForElement } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Overview from '../Overview';
import { renderWithProvider } from 'utils/testUtils';

afterEach(cleanup);

// Skip the tests for now. We should mock the XHR requests in the test
describe('<Overview />', () => {
  const imageName = generate.name();

  const props = {
    history: {
      push: jest.fn(),
    },
    worker: {
      name: generate.name(),
      brokerClusterName: generate.serviceName(),
      clientPort: generate.port(),
      jmxPort: generate.port(),
      imageName,
      connectors: [],
      nodeNames: [generate.name()],
    },
  };

  it('renders the page', async () => {
    await waitForElement(() => renderWithProvider(<Overview {...props} />));
  });

  it('renders the correct docker image name', async () => {
    const { getByText } = await renderWithProvider(<Overview {...props} />);

    getByText(`Image: ${imageName}`);
  });

  // The rest of the tests are covered in the end-to-end tests
  // since these tests require a LOT of mocking, it's probably to
  // test them in the end-to-end for now
});
