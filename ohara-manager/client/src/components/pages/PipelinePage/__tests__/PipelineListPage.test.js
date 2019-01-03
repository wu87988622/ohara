import React from 'react';
import toastr from 'toastr';
import { shallow } from 'enzyme';

import * as MESSAGES from 'constants/messages';
import PipelineListPage from '../PipelineListPage';
import { PIPELINE } from 'constants/documentTitles';
import { getTestById } from 'utils/testUtils';
import {
  createPipeline,
  deletePipeline,
  fetchPipelines,
} from 'apis/pipelinesApis';

jest.mock('apis/pipelinesApis');
jest.mock('utils/pipelineUtils');

const pipelines = [
  {
    name: 'a',
    status: 'Stopped',
    id: '1234',
    objects: [{ abc: 'def', kind: 'topic', id: '123' }],
  },
  {
    name: 'b',
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
    const newBtn = wrapper.find('NewPipelineBtn');
    const id = '1234';
    const expectedUrl = `${props.match.url}/new/${id}`;
    const res = { data: { result: { id } } };

    createPipeline.mockImplementation(() => Promise.resolve(res));
    await newBtn.prop('handleClick')();

    expect(toastr.success).toHaveBeenCalledTimes(1);
    expect(toastr.success).toHaveBeenCalledWith(
      MESSAGES.PIPELINE_CREATION_SUCCESS,
    );
    expect(props.history.push).toHaveBeenCalledTimes(1);
    expect(props.history.push).toHaveBeenCalledWith(expectedUrl);
  });

  it('renders <ConfirmModal />', () => {
    const modal = wrapper.find('ConfirmModal');
    const _props = modal.props();
    expect(modal.length).toBe(1);
    expect(_props.isActive).toBe(false);
    expect(_props.title).toBe('Delete pipeline?');
    expect(_props.cancelBtnText).toBe('No, Keep it');
    expect(_props.handleCancel).toBeDefined();
    expect(_props.handleConfirm).toBeDefined();
    expect(_props.message).toBe(
      'Are you sure you want to delete this pipeline? This action cannot be redo!',
    );
  });

  it('successfully deletes the first pipeline', async () => {
    wrapper.setState({ pipelines });
    const id = pipelines[0].id;
    const pipelineName = 'pipelineAbc';
    const res = { data: { result: { id, name: pipelineName } } };
    const expectedSuccessMsg = `${
      MESSAGES.PIPELINE_DELETION_SUCCESS
    } ${pipelineName}`;

    deletePipeline.mockImplementation(() => Promise.resolve(res));

    expect(wrapper.find('Table tr').length).toBe(2);

    wrapper
      .find(getTestById('delete-pipeline'))
      .at(0)
      .find('DeleteIcon')
      .prop('onClick')(id);

    expect(wrapper.find('ConfirmModal').props().isActive).toBe(true);
    await wrapper.find('ConfirmModal').prop('handleConfirm')();
    expect(deletePipeline).toHaveBeenCalledTimes(1);
    expect(deletePipeline).toHaveBeenCalledWith(id);
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
        return pipelines.map(({ name, status }, idx) => {
          return [String(idx), name, status, 'LinkIcon', 'DeleteIcon'];
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
