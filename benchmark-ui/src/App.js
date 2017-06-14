import React from 'react';
import 'bootstrap/less/bootstrap.less';
import './styles/custom-styles.css';

import RouplexHeader from './RouplexHeader';
import RouplexBody from './RouplexBody';
import RouplexFooter from './RouplexFooter';

//global.React = React;

class App extends React.Component {
  constructor() {
    super();

    this.state = {
      sessionInfo: {},
      actionPath: window.location.search.substr(1)
    };
  }

  handleSessionUpdate(newSessionInfo) {
    console.log("handleSessionUpdate: " + newSessionInfo);
    this.setState({
      sessionInfo: newSessionInfo ? newSessionInfo : {}
    })
  }

  handleActionPathUpdate(newActionPath) {
    console.log("handleNewActionPath: " + newActionPath);
    this.setState({
      actionPath: newActionPath ? newActionPath : {}
    })
  }

  render() {
    return (
      <div>
        <RouplexHeader
          sessionInfo={this.state.sessionInfo}
          onSessionUpdate={(newSessionInfo) => this.handleSessionUpdate(newSessionInfo)}/>
        <RouplexBody
          sessionInfo={this.state.sessionInfo}
          actionPath={this.state.actionPath}
          onActgionPathUpdate={(newActionPath) => this.handleActionPathUpdate(newActionPath)}/>
        <RouplexFooter />
      </div>
    )
  }
}

React.render(<App/>, document.getElementById('app'));
