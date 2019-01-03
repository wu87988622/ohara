import React from 'react';
import { shallow } from 'enzyme';

import * as URLS from 'constants/urls';
import Header from '../Header';
import NAVS from 'constants/navs';
import { fetchCluster } from 'apis/clusterApis';

jest.mock('apis/clusterApis');

fetchCluster.mockImplementation(() =>
  Promise.resolve({ data: { result: { versionInfo: {} }, isSuccess: true } }),
);

const props = {
  isLogin: false,
};

describe('<Header />', () => {
  let wrapper;

  beforeEach(() => {
    fetchCluster.mockImplementation(() =>
      Promise.resolve({
        data: {
          result: {
            versionInfo: {
              version: '123',
              revision: 'abcdefghijklno123',
              date: Date.now(),
            },
          },
          isSuccess: true,
        },
      }),
    );
    wrapper = shallow(<Header {...props} />);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('StyledHeader');
  });

  it('should not render if cluster info not provided', () => {
    fetchCluster.mockImplementation(() => Promise.resolve({}));

    wrapper = shallow(<Header {...props} />);
    expect(wrapper.name()).toBe(null);
  });

  it('renders <Brand />', () => {
    const brand = wrapper.find('Brand');
    const _props = brand.props();

    expect(brand.length).toBe(1);
    expect(_props.to).toBe(URLS.HOME);
    expect(brand.children().text()).toBe('Ohara');
  });

  it('renders <Nav />', () => {
    expect(wrapper.find('Nav').length).toBe(1);
  });

  it('renders log in link and text when this.state.isLogin is false', () => {
    const login = wrapper.find('Login');
    expect(login.props().to).toBe(URLS.LOGIN);
    expect(login.children().text()).toBe('Log in');
  });

  it('renders log out link and text when this.state.isLogin is true', () => {
    wrapper.setProps({ isLogin: true });

    const login = wrapper.find('Login');

    expect(login.props().to).toBe(URLS.LOGOUT);
    expect(login.children().text()).toBe('Log out');
  });

  it('renders Navigation <Link />', () => {
    const links = wrapper.find('Link');

    links.forEach((link, idx) => {
      const linkProps = link.props();
      const icon = link.find('Icon');
      const span = link.find('span');
      const iconProps = icon.props();

      expect(span.children().text()).toBe(NAVS[idx].text);
      expect(linkProps.exact).toBe(true);
      expect(linkProps.activeClassName).toBe('active');
      expect(linkProps.to).toBe(NAVS[idx].to);

      // TODO: change this to regex, this is prone to error
      expect(iconProps.className).toBe(`fas ${NAVS[idx].iconCls}`);
    });
  });
});
