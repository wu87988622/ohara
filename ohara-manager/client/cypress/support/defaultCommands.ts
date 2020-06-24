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

// indicate this file is a module
export {};

const SINGLE_COMMAND_TIMEOUT = 5000;

Cypress.Commands.overwrite('get', (originFn, subject, options) => {
  // we only wait a few seconds for simple command instead of using defaultCommandTimeout
  const customOptions = {
    ...options,
    timeout: SINGLE_COMMAND_TIMEOUT,
  };
  return originFn(subject, customOptions);
});

Cypress.Commands.overwrite('click', (originFn, subject, options) => {
  // we only wait a few seconds for simple command instead of using defaultCommandTimeout
  const customOptions = {
    timeout: SINGLE_COMMAND_TIMEOUT,
    ...options,
  };
  // The API of testing library "findByTitle" or "getByTitle" will try to query the element which has the "title=" attribute.
  // However, cypress click() command is a synthetic event (https://dev.to/gabbersepp/doing-native-clicks-with-cypress-io-and-open-file-dialog-18n6)
  // which will produce weird in our material UI ToolTip component
  // ex: cypress click the button which has tooltip by "findByTitle" -> do something operations -> try to "findByTitle" will never found
  // it may be a workaround to replace .click() by .trigger("click") for such cases...
  // more example could be found in appBar.test.ts
  if (subject?.selector && subject.selector.includes('ByTitle')) {
    return cy.wrap(subject).trigger('click', customOptions);
  }
  return originFn(subject, customOptions);
});

// referenced from: https://github.com/cypress-io/cypress/issues/871#issuecomment-509392310
// When scrolling an element into view, the click callback function may have problem
// this is a workaround for now until the issue has been resolved...
Cypress.on('scrolled', ($el) => {
  $el.get(0).scrollIntoView({
    block: 'center',
    inline: 'center',
  });
});
