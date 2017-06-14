import React from 'react';
import 'bootstrap/less/bootstrap.less';
import './styles/custom-styles.css';

import Header from './Header';
import Body from './Body';
import Footer from './Footer';

//global.React = React;

class App extends React.Component {
  constructor() {
    super();

    this.state = {
      sessionInfo: {},
      actionPath: window.location.search.substr(1)
    };
  }

  render() {
    return (
      <div>
        <Header
          sessionInfo={this.state.sessionInfo}
          onSessionUpdate={sessionInfo => this.setState({sessionInfo: sessionInfo})}/>
        <Body
          sessionInfo={this.state.sessionInfo}
          actionPath={this.state.actionPath}
          onActionPathUpdate={actionPath => this.setState({actionPath: actionPath})}/>
        <Footer />
      </div>
    )
  }
}

React.render(<App/>, document.getElementById('app'));
