import React from 'react';
import ReactDom from 'react-dom';
import 'bootstrap/less/bootstrap.less';
import './styles/custom-styles.css';

import Header from './Header';
import Body from './Body';
import Footer from './Footer';

var config = require("./Config.js");

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
          onSessionUpdate={sessionInfo => {
            if (config.autologin && !sessionInfo.userInfo) {
              console.log("faking logged in user");
              sessionInfo.userInfo = "fake dev user";
            }

            this.setState({sessionInfo: sessionInfo});
          }}/>

        <Body
          sessionInfo={this.state.sessionInfo}
          actionPath={this.state.actionPath}
          onActionPathUpdate={actionPath => this.setState({actionPath: actionPath})}
        />

        <Footer/>
      </div>
    )
  }
}

ReactDom.render(<App/>, document.getElementById('app'));
