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
import '@testing-library/jest-dom/extend-expect';

import WorkspacesNewModal from '../WorkspacesNewModal';
import { renderWithProvider } from 'utils/testUtils';
import * as useApi from 'components/controller';
import * as generate from 'utils/generate';

jest.mock('components/controller');

describe('<WorkspacesNewModal />', () => {
  let props;
  beforeEach(() => {
    props = {
      isActive: true,
      onClose: jest.fn(),
      onConfirm: jest.fn(),
    };

    jest.spyOn(useApi, 'useUploadApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'useDeleteApi').mockImplementation(() => {
      return { deleteApi: jest.fn() };
    });

    jest.spyOn(useApi, 'usePutApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'useWaitApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'usePostApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(() => {
      return {
        data: {
          data: {
            result: [
              {
                name: generate.name(),
              },
            ],
          },
        },
        isLoading: false,
      };
    });
  });

  it('renders the modal', () => {
    renderWithProvider(<WorkspacesNewModal {...props} />);
  });
});
