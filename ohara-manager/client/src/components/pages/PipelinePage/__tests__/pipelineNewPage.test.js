import React from 'react';
import uuid from 'uuid';
import { shallow } from 'enzyme';

import PipelineNewPage from '../PipelineNewPage';
import { PIPELINE } from 'constants/urls';
import { PIPELINE_NEW } from 'constants/documentTitles';

const props = {
  match: {
    params: {
      topicId: uuid.v4(),
    },
  },
};

describe('<PipelineNewPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelineNewPage {...props} />);

    // TODO: change this to a more real world like case, e.g., mock data returns by some requests
    wrapper.setState({
      pipelines: { name: 'test' },
    });
  });

  it('renders self', () => {
    expect(wrapper.find('Wrapper').length).toBe(1);
  });

  it('renders the correct document title', () => {
    expect(wrapper.props().title).toBe(PIPELINE_NEW);
  });

  it('should render <Redirect /> when topicUuid is not present', () => {
    const match = {};
    wrapper = shallow(<PipelineNewPage match={match} />);
    expect(wrapper.props().to).toBe(PIPELINE);
  });

  it('renders the <H2 />', () => {
    expect(wrapper.find('H2').length).toBe(1);
  });

  it('renders <Toolbar />', () => {
    expect(wrapper.find('PipelineToolbar').length).toBe(1);
  });

  it('renders <PipelineGraph />', () => {
    expect(wrapper.find('PipelineGraph').length).toBe(1);
  });

  it('renders 5 <Route />', () => {
    expect(wrapper.find('Route').length).toBe(5);
  });
});
