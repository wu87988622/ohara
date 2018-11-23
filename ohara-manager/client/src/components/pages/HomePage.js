import React from 'react';
import { Redirect } from 'react-router-dom';
import * as URLS from '../../constants/urls';

class HomePage extends React.Component {
  render() {
    return <Redirect to={URLS.PIPELINE} />;
  }
}
export default HomePage;
