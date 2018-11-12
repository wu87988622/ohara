import React from 'react';
import toastr from 'toastr';
import { shallow } from 'enzyme';

import Configuration from '../ConfigurationPage.js';
import * as MESSAGES from 'constants/messages';
import { CONFIGURATION } from 'constants/documentTitles';
import { primaryBtn, cancelBtn } from 'theme/btnTheme';
import { getTestById } from 'utils/testHelpers';
import { validateHdfs, saveHdfs, fetchHdfs } from 'apis/configurationApis';

jest.mock('apis/configurationApis');

const props = { history: { goBack: jest.fn() } };

describe('<Configuration />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Configuration {...props} />);
    jest.clearAllMocks();
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(CONFIGURATION);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Configuration');
  });

  it('renders Name <FormGroup />', () => {
    const form = wrapper.find(getTestById('connection-name'));
    const label = form.find('Label');
    const input = form.find('Input');
    const inputProps = input.props();

    expect(form.length).toBe(1);
    expect(label.children().text()).toBe('Name');
    expect(input.length).toBe(1);

    expect(inputProps.name).toBe('connectionName');
    expect(inputProps.width).toBe('250px');
    expect(inputProps.placeholder).toBe('Connection name');
    expect(inputProps.value).toBe(wrapper.state().connectionName);
    expect(inputProps.handleChange).toBeDefined();
  });

  it('renders HDFS connection URL <FormGroup />', () => {
    const form = wrapper.find(getTestById('connection-url'));
    const label = form.find('Label');
    const input = form.find('Input');
    const inputProps = input.props();

    expect(form.length).toBe(1);
    expect(label.children().text()).toBe('HDFS connection URL');
    expect(input.length).toBe(1);

    expect(inputProps.name).toBe('connectionUrl');
    expect(inputProps.width).toBe('250px');
    expect(inputProps.placeholder).toBe('http://localhost:5050');
    expect(inputProps.value).toBe(wrapper.state().connectionUrl);
    expect(inputProps.handleChange).toBeDefined();
  });

  it('renders <CancelBtn />', () => {
    const btn = wrapper.find(getTestById('cancel-btn'));
    const _props = btn.props();

    expect(btn.length).toBe(1);
    expect(_props.text).toBe('Cancel');
    expect(_props.theme).toBe(cancelBtn);
    expect(_props.handleClick).toBeDefined();
  });

  it('renders <Button>', () => {
    const btn = wrapper.find(getTestById('test-connection-btn'));
    const _props = btn.props();
    const { isWorking, isBtnDisabled } = wrapper.state();

    expect(btn.length).toBe(1);
    expect(_props.text).toBe('Test connection');
    expect(_props.isWorking).toBe(isWorking);
    expect(_props.theme).toBe(primaryBtn);
    expect(_props.disabled).toBe(isBtnDisabled);
    expect(_props.handleClick).toBeDefined();
  });

  it('displays the <Prompt /> if <Button /> is clicked', () => {
    const evt = { preventDefault: jest.fn() };

    expect(wrapper.find('Prompt').props().when).toBe(false);
    wrapper.find(getTestById('test-connection-btn')).prop('handleClick')(evt);
    expect(evt.preventDefault).toHaveBeenCalledTimes(1);
    expect(wrapper.find('Prompt').props().when).toBe(true);
  });

  it('displays the <Prompt /> if Name <Input /> is updated', () => {
    const evt = { target: { value: 'a', name: 'b' } };
    expect(wrapper.find('Prompt').props().when).toBe(false);
    wrapper.find(getTestById('connection-name-input')).prop('handleChange')(
      evt,
    );

    expect(wrapper.find('Prompt').props().when).toBe(true);
  });

  it('displays the <Prompt /> if HDFS connection URL <Input /> is updated', () => {
    const evt = { target: { value: 'a', name: 'b' } };
    expect(wrapper.find('Prompt').props().when).toBe(false);
    wrapper.find(getTestById('connection-url-input')).prop('handleChange')(evt);

    expect(wrapper.find('Prompt').props().when).toBe(true);
  });

  it('goes to previous page if <CancelBtn /> is clicked', () => {
    const evt = { preventDefault: jest.fn() };
    wrapper.find(getTestById('cancel-btn')).prop('handleClick')(evt);
    expect(evt.preventDefault).toHaveBeenCalledTimes(1);
    expect(props.history.goBack).toHaveBeenCalledTimes(1);
  });

  it('displays a success messages if test connection and saving config both actions passed', async () => {
    const evt = { preventDefault: jest.fn() };
    const name = 'abc';
    const url = '123';
    const saveRes = { data: { isSuccess: true } };
    const validateRes = { data: { isSuccess: true } };

    wrapper.setState({ connectionName: name, connectionUrl: url });
    saveHdfs.mockImplementation(() => Promise.resolve(saveRes));
    validateHdfs.mockImplementation(() => Promise.resolve(validateRes));
    await wrapper.find(getTestById('test-connection-btn')).prop('handleClick')(
      evt,
    );

    expect(evt.preventDefault).toHaveBeenCalledTimes(1);
    expect(validateHdfs).toHaveBeenCalledTimes(1);
    expect(validateHdfs).toHaveBeenCalledWith({ uri: url });
    expect(saveHdfs).toHaveBeenCalledTimes(1);
    expect(saveHdfs).toHaveBeenCalledWith({
      name: name,
      uri: url,
    });
    expect(toastr.success).toHaveBeenCalledTimes(2);
    expect(toastr.success).toHaveBeenCalledWith(MESSAGES.TEST_SUCCESS);
    expect(toastr.success).toHaveBeenCalledWith(MESSAGES.CONFIG_SAVE_SUCCESS);
    expect(wrapper.find('Prompt').props().when).toBe(false);
  });

  it('allows user to click on the <Button /> if both name and url fields are filled out', () => {
    const nameEvt = { target: { value: 'abc', name: 'connectionName' } };
    const urlEvt = { target: { value: '/123', name: 'connectionUrl' } };

    expect(
      wrapper.find(getTestById('test-connection-btn')).props().disabled,
    ).toBe(true);

    wrapper.find(getTestById('connection-name-input')).prop('handleChange')(
      nameEvt,
    );

    wrapper.find(getTestById('connection-url-input')).prop('handleChange')(
      urlEvt,
    );

    expect(
      wrapper.find(getTestById('test-connection-btn')).props().disabled,
    ).toBe(false);
  });

  it('fetches the data on mount', async () => {
    const res = {
      data: {
        isSuccess: true,
        result: [
          { uri: '/123', name: 'a', lastModified: 22 },
          { uri: '/234', name: 'b', lastModified: 11 },
        ],
      },
    };

    fetchHdfs.mockImplementation(() => Promise.resolve(res));
    wrapper = await shallow(<Configuration {...props} />);

    expect(fetchHdfs).toHaveBeenCalledTimes(1);
    expect(
      wrapper.find(getTestById('test-connection-btn')).props().disabled,
    ).toBe(false);
  });
});
