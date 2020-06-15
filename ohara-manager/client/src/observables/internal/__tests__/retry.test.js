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

import { TestScheduler } from 'rxjs/testing';
import { retry } from '../retry';

describe('retry operator', () => {
  let testScheduler;

  beforeEach(() => {
    testScheduler = new TestScheduler((actual, expected) => {
      expect(actual).toEqual(expected);
    });
  });

  it('should handle a basic source that emits next then errors, maxRetries 3', () => {
    testScheduler.run(({ expectObservable, cold, expectSubscriptions }) => {
      const source = cold('--1-2-3-#');
      const subs = [
        '                  ^-------!',
        '                  ---------^-------!',
        '                  -------------------^-------!',
        '                  -------------------------------^-------!',
      ];
      const expected = '   --1-2-3----1-2-3-----1-2-3-------1-2-3-#';

      expectObservable(
        source.pipe(
          retry({
            delay: 1,
            maxRetries: 3,
          }),
        ),
      ).toBe(expected);
      expectSubscriptions(source.subscriptions).toBe(subs);
    });
  });

  it('should handle a basic source that emits next then completes', () => {
    testScheduler.run(({ expectObservable, hot, expectSubscriptions }) => {
      const source = hot('--1--2--^--3--4--5---|');
      const subs = '              ^------------!';
      const expected = '          ---3--4--5---|';

      const result = source.pipe(retry(1));

      expectObservable(result).toBe(expected);
      expectSubscriptions(source.subscriptions).toBe(subs);
    });
  });

  it('should handle a basic source that emits next but does not complete', () => {
    testScheduler.run(({ expectObservable, hot, expectSubscriptions }) => {
      const source = hot('--1--2--^--3--4--5---');
      const subs = '              ^------------';
      const expected = '          ---3--4--5---';

      const result = source.pipe(retry(1));

      expectObservable(result).toBe(expected);
      expectSubscriptions(source.subscriptions).toBe(subs);
    });
  });

  it('should handle a basic source that emits next then errors, no maxRetries', () => {
    testScheduler.run(({ expectObservable, cold, expectSubscriptions }) => {
      const source = cold('--1-2-3-#');
      const unsub = '      -------------------------------------!';
      const subs = [
        '                  ^-------!                             ',
        '                  ---------^-------!                    ',
        '                  -------------------^-------!          ',
        '                  -------------------------------^-----!',
      ];
      const expected = '   --1-2-3----1-2-3-----1-2-3-------1-2--';

      const result = source.pipe(retry(1));

      expectObservable(result, unsub).toBe(expected);
      expectSubscriptions(source.subscriptions).toBe(subs);
    });
  });

  it('should handle a source which eventually throws, maxRetries=3, and result is unsubscribed early', () => {
    testScheduler.run(({ expectObservable, cold, expectSubscriptions }) => {
      const source = cold('--1-2-3-#');
      const unsub = '      -------------!';
      const subs = [
        '                  ^-------!',
        '                  ---------^---!',
      ];
      const expected = '   --1-2-3----1--';

      const result = source.pipe(retry({ delay: 1, maxRetries: 3 }));

      expectObservable(result, unsub).toBe(expected);
      expectSubscriptions(source.subscriptions).toBe(subs);
    });
  });

  it('should increase the intervals exponentially up to maxInterval', () => {
    testScheduler.run(({ expectObservable, cold, expectSubscriptions }) => {
      const source = cold('--1-2-3-#');
      const subs = [
        '                  ^-------!',
        '                  ---------^-------!',
        '                  -------------------^-------!',
        '                  -----------------------------^-------!',
        //                      interval maxed out at 2 ^
      ];
      const unsub = '      -------------------------------------!';
      const expected = '   --1-2-3----1-2-3-----1-2-3-----1-2-3--';

      expectObservable(
        source.pipe(retry({ delay: 1, maxDelay: 2 })),
        unsub,
      ).toBe(expected);
      expectSubscriptions(source.subscriptions).toBe(subs);
    });
  });

  it('should increase the intervals calculated by delayStrategy function', () => {
    testScheduler.run(({ expectObservable, cold, expectSubscriptions }) => {
      const constantDelay = (iteration, delay) => delay;
      const source = cold('-1-#');
      const subs = [
        '                  ^--!',
        '                  ----^--!',
        '                  --------^--!',
        '                  ------------^--!',
        '                  ----------------^--!',
      ];
      const unsub = '      -------------------!';
      const expected = '   -1---1---1---1---1--';

      expectObservable(
        source.pipe(
          retry({
            delay: 1,
            delayStrategy: constantDelay,
          }),
        ),
        unsub,
      ).toBe(expected);
      expectSubscriptions(source.subscriptions).toBe(subs);
    });
  });
});
