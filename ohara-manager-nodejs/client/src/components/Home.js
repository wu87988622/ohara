import React from 'react';

import AppWrapper from './common/AppWrapper';
import Header from './common/Header';
import Button from './common/Button';

class Home extends React.Component {
  render() {
    return (
      <AppWrapper>
        <Header />
        <p className="App-intro">
          To get started, edit <code>src/App.js</code> and save to reload.
        </p>
        <Button>Hello there!</Button>
      </AppWrapper>
    );
  }
}

export default Home;
