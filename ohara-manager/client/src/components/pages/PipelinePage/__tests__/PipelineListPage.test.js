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
import toastr from 'toastr';
import { shallow } from 'enzyme';

import * as MESSAGES from 'constants/messages';
import PipelineListPage from '../PipelineListPage';
import { PIPELINE } from 'constants/documentTitles';
import { getTestById } from 'utils/testUtils';
import { fetchWorkers } from 'api/workerApi';
import {
  createPipeline,
  deletePipeline,
  fetchPipelines,
} from 'api/pipelineApi';

jest.mock('api/pipelineApi');
jest.mock('../pipelineUtils/commonUtils');
jest.mock('api/workerApi');

const pipelines = [
  {
    name: 'a',
    workerClusterName: 'worker-a',
    status: 'Stopped',
    id: '1234',
    objects: [{ abc: 'def', kind: 'topic', id: '123' }],
  },
  {
    name: 'b',
    workerClusterName: 'worker-b',
    status: 'Running',
    id: '5678',
    objects: [{ def: 'abc', kind: 'topic', id: '456' }],
  },
];

fetchPipelines.mockImplementation(() => Promise.resolve(pipelines));

const props = {
  match: {
    url: '/to/a/new/page',
  },
  history: { push: jest.fn() },
};

describe('<PipelineListPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelineListPage {...props} />);
    jest.clearAllMocks();
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(PIPELINE);
  });

  it('renders a loading indicator when the pipeline data is still loading', () => {
    wrapper.setState({ isFetchingPipeline: true });
    expect(wrapper.find('TableLoader').length).toBe(1);

    wrapper.setState({ isFetchingPipeline: false });
    expect(wrapper.find('TableLoader').length).toBe(0);
  });

  it('renders <H2 />', () => {
    const h2 = wrapper.find('H2');
    expect(h2.length).toBe(1);
    expect(h2.children().text()).toBe('Pipelines');
  });

  it('renders <NewPipelineBtn>', () => {
    expect(wrapper.find('NewPipelineBtn').length).toBe(1);
    expect(wrapper.find('NewPipelineBtn').props().text).toBe('New pipeline');
  });

  it('creates a new pipeline', async () => {
    const evt = { preventDefault: jest.fn() };
    const name = '1234';
    const expectedUrl = `${props.match.url}/new/${name}`;
    const res = { data: { result: { name } } };
    const workersRes = {
      data: {
        result: [
          {
            name: 'worker-1',
          },
          {
            name: 'worker-2',
          },
        ],
      },
    };

    createPipeline.mockImplementation(() => Promise.resolve(res));
    fetchWorkers.mockImplementation(() => Promise.resolve(workersRes));

    wrapper.find('NewPipelineBtn').prop('handleClick')(evt);

    expect(wrapper.find('Modal').props().isActive).toBe(true);

    await wrapper.find('Modal').prop('handleConfirm')();

    expect(wrapper.find('Modal').props().isActive).toBe(false);
    expect(toastr.success).toHaveBeenCalledTimes(1);
    expect(toastr.success).toHaveBeenCalledWith(
      MESSAGES.PIPELINE_CREATION_SUCCESS,
    );
    expect(props.history.push).toHaveBeenCalledTimes(1);
    expect(props.history.push).toHaveBeenCalledWith(expectedUrl);
  });

  it(`toggles <Modal />'s confirm button state if there's no worker cluster present`, async () => {
    const workersRes = {
      data: {
        result: [
          {
            name: 'worker-1',
          },
        ],
      },
    };

    wrapper.setState({ workers: [], currWorker: {} });
    expect(wrapper.find('Modal').props().isConfirmDisabled).toBe(true);

    fetchWorkers.mockImplementation(() => Promise.resolve(workersRes));
    wrapper = await shallow(<PipelineListPage {...props} />);

    expect(wrapper.find('Modal').props().isConfirmDisabled).toBe(false);
  });

  it('renders <ConfirmModal />', () => {
    const modal = wrapper.find('AlertDialog');
    const _props = modal.props();

    expect(modal.length).toBe(1);
    expect(_props.open).toBe(false);
  });

  it('successfully deletes the first pipeline', async () => {
    wrapper.setState({ pipelines });
    const pipelineName = pipelines[0].name;
    const res = {
      data: { result: { name: pipelineName }, isSuccess: true },
    };
    const expectedSuccessMsg = `${MESSAGES.PIPELINE_DELETION_SUCCESS} ${pipelineName}`;

    deletePipeline.mockImplementation(() => Promise.resolve(res));

    expect(wrapper.find('Table tr').length).toBe(2);

    wrapper
      .find(getTestById('delete-pipeline'))
      .at(0)
      .find('DeleteIcon')
      .prop('onClick')(pipelineName);

    expect(wrapper.find('AlertDialog').props().open).toBe(true);
    await wrapper.find('AlertDialog').prop('handleConfirm')();
    expect(deletePipeline).toHaveBeenCalledTimes(1);
    expect(deletePipeline).toHaveBeenCalledWith(pipelineName);
    expect(toastr.success).toHaveBeenCalledTimes(1);
    expect(toastr.success).toHaveBeenCalledWith(expectedSuccessMsg);
    expect(wrapper.find('Table tr').length).toBe(1);
  });

  describe('<DataTable />', () => {
    let table;
    beforeEach(() => {
      wrapper.setState({ pipelines });
      table = wrapper.find('Table');
    });

    it('renders self', () => {
      expect(table.length).toBe(1);
    });

    it('renders the correct rows', () => {
      const rows = table.find('tr');
      expect(rows.length).toBe(pipelines.length);
    });

    it('renders <tr /> and <td />', () => {
      const table = wrapper.find('Table');
      const rows = table.find('tr');

      const firstRow = rows.at(0).find('td');
      const secondRow = rows.at(1).find('td');

      const createExpects = pipelines => {
        return pipelines.map(({ name, workerClusterName, status }) => {
          return [name, workerClusterName, status, 'LinkIcon', 'DeleteIcon'];
        });
      };

      const expected = createExpects(pipelines);

      firstRow.forEach((x, idx) => {
        if (!x.props().className) {
          expect(x.text()).toBe(expected[0][idx]);
        } else if (x.props().className === 'has-icon') {
          expect(x.children().name()).toBe(expected[0][idx]);
        }
      });

      // TODO: add tests for LinkIcon and DeleteIcon
      secondRow.forEach((x, idx) => {
        if (!x.props().className) {
          expect(x.text()).toBe(expected[1][idx]);
        } else if (x.props().className === 'has-icon') {
          expect(x.children().name()).toBe(expected[1][idx]);
        }
      });
    });
  });
});
