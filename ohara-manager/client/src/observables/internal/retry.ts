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

import { defer, iif, Observable, throwError, timer } from 'rxjs';
import { concatMap, retryWhen } from 'rxjs/operators';
import { exponentialDelayStrategy } from './strategies';
import { getDelay } from './utils';

export interface RetryConfig {
  // A delay in milliseconds. It will eventually go as high as maxDelay.
  delay?: number;
  // Maximum number of retry attempts.
  maxRetries?: number;
  // Maximum delay between retries.
  maxDelay?: number;
  // Conditional retry.
  shouldRetry?: (error: any) => boolean;
  delayStrategy?: (iteration: number, initialInterval: number) => number;
}

/**
 * Returns an Observable that mirrors the source Observable with the exception
 * of an error. Retrying can be cancelled at any point if shouldRetry returns
 * false.
 */
export function retry(
  config: RetryConfig | number,
): <T>(source: Observable<T>) => Observable<T> {
  const {
    delay = 1000,
    maxRetries = Infinity,
    maxDelay = Infinity,
    shouldRetry = () => true,
    delayStrategy = exponentialDelayStrategy,
  } = typeof config === 'number' ? { delay: config } : config;

  return <T>(source: Observable<T>) =>
    defer(() => {
      let index = 0;
      return source.pipe(
        retryWhen<T>((errors) =>
          errors.pipe(
            concatMap((error) => {
              const attempt = index++;
              return iif(
                () => attempt < maxRetries && shouldRetry(error),
                timer(getDelay(delayStrategy(attempt, delay), maxDelay)),
                throwError(error),
              );
            }),
          ),
        ),
      );
    });
}
