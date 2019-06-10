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
import { handleError, getErrors } from '../apiUtils';

describe('handleError()', () => {
  afterEach(jest.clearAllMocks);
  it('handles error with message key ', () => {
    const message = 'Oops';
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

  it('handle error which contains fieldName key', () => {
    const errors = [generate.name(), generate.name()];
    const fieldName = generate.name();
    const message = [{ fieldName, errors }];
    const expectedMsg = `1: ${errors[0]} <br />2: ${errors[1]} <br />`;
    const expectedTitle = fieldName;

    const err = {
      data: {
        errorMessage: {
          message,
        },
      },
    };

    handleError(err);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(expectedMsg, expectedTitle);
  });

  it('handles error with errorMessage key', () => {
    const errorMessage = 'Nah';
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
    const err = `I'm an error`;

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
});

describe('getErrors()', () => {
  it('return errors for normal connectors', () => {
    const data = [
      { hostname: generate.ip(), message: generate.message(), pass: false },
      { hostname: generate.ip(), message: generate.message(), pass: true },
    ];

    expect(getErrors(data)).toEqual([data[0]]);
  });

  it(`return an empty array if there's no errors`, () => {
    const data = [
      { hostname: generate.ip(), message: generate.message(), pass: true },
      { hostname: generate.ip(), message: generate.message(), pass: true },
      { hostname: generate.ip(), message: generate.message(), pass: true },
      { hostname: generate.ip(), message: generate.message(), pass: true },
    ];

    expect(getErrors(data)).toEqual([]);
  });

  it('returns errors for custom connectors', () => {
    const displayName = generate.name();
    const errors = [generate.message()];

    const data = {
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

    const expectedErrors = [
      {
        fieldName: displayName,
        errors,
      },
    ];

    expect(getErrors(data)).toEqual(expectedErrors);
  });
});
