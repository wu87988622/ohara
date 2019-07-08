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
import { handleError } from '../apiUtils';

describe('handleError()', () => {
  afterEach(jest.clearAllMocks);
  it('handles error with message key ', () => {
    const message = generate.message();
    const err = {
      data: {
        errorMessage: {
          message,
        },
      },
    };

    handleError(err);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(message);
  });

  it('handles error which contains multiple messages', () => {
    const message = [
      {
        pass: false,
        message: generate.message(),
      },
      {
        pass: false,
        message: generate.message(),
      },
      {
        pass: true,
        message: generate.message(),
      },
    ];

    const err = {
      data: {
        errorMessage: {
          message,
        },
      },
    };

    handleError(err);
    expect(toastr.error).toHaveBeenCalledTimes(2);
  });

  it('handles error with errorMessage key', () => {
    const errorMessage = generate.message();
    const err = {
      data: {
        errorMessage,
      },
    };

    handleError(err);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(errorMessage);
  });

  it(`handles error when err object itself is string`, () => {
    const err = generate.message();

    handleError(err);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(err);
  });

  it('returns a custom error message when err is undefined', () => {
    let err;
    handleError(err);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith('Internal Server Error');
  });

  it('returns errors for validation', () => {
    const displayName = generate.name();
    const errors = [generate.message(), generate.message()];

    const err = {
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

    handleError(err);
    const errorMessage = errors.join(' ');

    const expectedErrors = `<b>${displayName.toUpperCase()}</b><br /> ${errorMessage}`;

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(expectedErrors);
  });
});
