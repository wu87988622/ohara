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

import toastr from 'toastr';

import * as generate from 'utils/generate';
import * as utils from '../apiUtils';

afterEach(jest.clearAllMocks);

describe('handleError()', () => {
  it('handles the error with a message key ', () => {
    const message = generate.message();
    const error = {
      data: {
        errorMessage: {
          message,
        },
      },
    };

    utils.handleError(error);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(message);
  });

  it('handles the error with a errorMessage key', () => {
    const errorMessage = generate.message();
    const error = {
      data: {
        errorMessage,
      },
    };

    utils.handleError(error);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(errorMessage);
  });

  it('handles the error when the given error object itself is string', () => {
    const error = generate.message();

    utils.handleError(error);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(error);
  });

  it('does nothing when the given error is undefined', () => {
    let error;
    utils.handleError(error);

    expect(toastr.error).toHaveBeenCalledTimes(0);
  });
});

describe('handleConnectorValidationError()', () => {
  it('handles the error which contains multiple messages', () => {
    const error = [
      {
        hostname: generate.name(),
        pass: false,
        message: generate.message(),
      },
      {
        hostname: generate.name(),
        pass: false,
        message: generate.message(),
      },
      {
        hostname: generate.name(),
        pass: true,
        message: generate.message(),
      },
    ];

    utils.handleNodeValidationError(error);
    expect(toastr.error).toHaveBeenCalledTimes(2);
  });

  it('returns errors for validation', () => {
    const displayName = generate.name();
    const errors = [generate.message(), generate.message()];

    const error = {
      errorCount: 1,
      settings: [
        {
          definition: { displayName },
          value: {
            key: generate.name(),
            value: generate.name(),
            errors,
          },
        },
        {
          definition: { displayName: generate.name() },
          value: { key: generate.name(), value: generate.name(), errors: [] },
        },
      ],
    };

    utils.handleConnectorValidationError(error);
    const errorMessage = errors.join(' ');

    const expectedErrors = `<b>${displayName.toUpperCase()}</b><br /> ${errorMessage}`;

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(expectedErrors);
  });
});
