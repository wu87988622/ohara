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
import {
  cleanup,
  render,
  waitForElement,
  fireEvent,
} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Topics from '../Topics';
import { renderWithTheme } from 'utils/testUtils';
import { fetchTopics, deleteTopic } from 'api/topicApi';

jest.mock('api/topicApi');

afterEach(cleanup);

describe('<Topics />', () => {
  let props;
  let brokerClusterName;
  let topics;
  beforeEach(() => {
    brokerClusterName = generate.serviceName();
    topics = generate.topics({ brokerClusterName });
    props = {
      match: {
        url: generate.url(),
      },
      worker: {
        brokerClusterName,
      },
    };

    const res = { data: { result: topics } };
    fetchTopics.mockImplementation(() => Promise.resolve(res));
  });

  it('renders the page', async () => {
    await render(<Topics {...props} />);
  });

  it('should properly render the table data', async () => {
    const { getByTestId } = await render(<Topics {...props} />);

    const topicName = getByTestId('topic-name').textContent;
    const partitions = Number(getByTestId('topic-partitions').textContent);
    const replications = Number(getByTestId('topic-replication').textContent);
    const lastModified = getByTestId('topic-lastModified').textContent;

    expect(topicName).toBe(topics[0].name);
    expect(partitions).toBe(topics[0].numberOfPartitions);
    expect(replications).toBe(topics[0].numberOfReplications);

    // It's hard to assert the output date format since the topic last modified
    // date is generated. So we're asserting it with any given string here.
    expect(lastModified).toEqual(expect.any(String));
  });

  it('renders multiple topics', async () => {
    const topics = generate.topics({ count: 5, brokerClusterName });

    const res = { data: { result: topics } };
    fetchTopics.mockImplementation(() => Promise.resolve(res));

    const { getAllByTestId } = await render(<Topics {...props} />);

    const topicNames = await waitForElement(() => getAllByTestId('topic-name'));
    expect(topicNames.length).toBe(topics.length);
  });

  it('should close the new topic modal with cancel button', async () => {
    const { getByText, queryByTestId } = await renderWithTheme(
      <Topics {...props} />,
    );

    fireEvent.click(getByText('New topic'));
    expect(queryByTestId('topic-new-modal')).toBeVisible();

    fireEvent.click(getByText('Cancel'));
    expect(queryByTestId('topic-new-modal')).toBeNull();
  });

  it('should close the delete dialog with the cancel button', async () => {
    deleteTopic.mockImplementation(() => Promise.resolve({}));

    const { getByTestId, getByText } = await renderWithTheme(
      <Topics {...props} />,
    );
    const topic = getByTestId(topics[0].name);

    fireEvent.click(topic);
    expect(getByText('Delete topic?')).toBeVisible(); // Ensure the modal is opened by getting it's title

    fireEvent.click(getByText('Cancel'));

    expect(deleteTopic).toHaveBeenCalledTimes(0);
    expect(getByText('Delete topic?')).not.toBeVisible();
  });
});
