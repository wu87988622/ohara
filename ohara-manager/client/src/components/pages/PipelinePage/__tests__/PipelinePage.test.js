import React from 'react';
import { shallow } from 'enzyme';

import PipelinePage from '../PipelinePage';

const pipelines = [
  {
    name: 'a',
    status: 'Stopped',
    uuid: '1',
    objects: [{ abc: 'def', kind: 'topic', uuid: '123' }],
  },
  {
    name: 'b',
    status: 'Running',
    uuid: '2',
    objects: [{ def: 'abc', kind: 'topic', uuid: '456' }],
  },
];

const props = {
  match: {
    url: '/to/a/new/page',
  },
};

describe('<PipelinePage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelinePage {...props} />);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <H2 />', () => {
    expect(wrapper.find('H2').length).toBe(1);
    expect(
      wrapper
        .find('H2')
        .children()
        .text(),
    ).toBe('Pipeline');
  });

  it('renders <NewPipelineBtn>', () => {
    expect(wrapper.find('NewPipelineBtn').length).toBe(1);
    expect(wrapper.find('NewPipelineBtn').props().text).toBe('New pipeline');
  });

  it('renders <Modal />', () => {
    const modal = wrapper.find('Modal');
    expect(modal.length).toBe(1);
    expect(modal.props().isActive).toBe(false);
  });

  it('toggles <Modal />', () => {
    const evt = { preventDefault: jest.fn() };

    wrapper.instance().handleSelectTopicModalOpen(evt);
    expect(wrapper.find('Modal').props().isActive).toBe(true);
    expect(evt.preventDefault).toHaveBeenCalledTimes(1);

    wrapper.instance().handleSelectTopicModalClose();
    expect(wrapper.find('Modal').props().isActive).toBe(false);
  });

  it('toggles disabled props to <Modal /> based on this.state.topic length', () => {
    expect(wrapper.find('Modal').props().isConfirmDisabled).toBe(true);
    wrapper.setState({ topics: [{ name: 'test', uuid: '2' }] });
    expect(wrapper.find('Modal').props().isConfirmDisabled).toBe(false);
  });

  it('renders <ConfirmModal />', () => {
    const modal = wrapper.find('ConfirmModal');
    expect(modal.length).toBe(1);
    expect(modal.props().isActive).toBe(false);
  });

  it('toggles <ConfirmModal />', () => {
    wrapper.setState({ pipelines });

    const uuid = '1234';
    wrapper.instance().handleDeletePipelineModalOpen(uuid);

    expect(wrapper.find('ConfirmModal').props().isActive).toBe(true);
    expect(wrapper.state().deletePipelineUuid).toBe(uuid);

    wrapper.instance().handleDeletePipelineModalClose();

    expect(wrapper.find('ConfirmModal').props().isActive).toBe(false);
    expect(wrapper.state().deletePipelineUuid).toBe('');
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
          return [
            String(idx),
            name,
            status,
            'StartStopIcon',
            'LinkIcon',
            'DeleteIcon',
          ];
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

      const startStopIcon = firstRow.find('StartStopIcon');
      expect(startStopIcon.props().isRunning).toBe(false);
      expect(startStopIcon.find('i').hasClass('fa-stop-circle'));

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
