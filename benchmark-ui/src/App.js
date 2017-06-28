import React from 'react';
import ReactDom from 'react-dom';
import 'bootstrap/less/bootstrap.less';
import './styles/custom-styles.css';

import Header from './Header';
import Body1 from './Body1';
import Footer from './Footer';
import Button from 'react-bootstrap/lib/Button';

var config = require("./Config.js");

class App extends React.Component {
  constructor() {
    super();

    this.state = {
      sessionInfo: {},
      path: config.getSanitizedPath()
    };

    window.onpopstate = (event) => {
      if (event.state) {
        this.setState({path: event.state});
      } else {
        window.history.back();
      }
    }
  }

  handlePathUpdate(path) {
    //alert("redirecting: " + path);

    try {
      window.history.pushState(path, "Rouplex-Demo " + path, config.baseUrl + "/?" + path);
    } catch (err) {
      // either non html5 or we are in dev mode loading page from file://
    }

    this.setState({path: path});
  }

  render() {
    return (
      <div>
        <Header
          path={this.state.path}
          sessionInfo={this.state.sessionInfo}
          onPathUpdate={path => this.handlePathUpdate(path)}
          onSessionUpdate={sessionInfo => this.setState({sessionInfo: sessionInfo ? sessionInfo : {}})}
        />

        <Body1
          path={this.state.path}
          sessionInfo={this.state.sessionInfo}
          onPathUpdate={path => this.handlePathUpdate(path)}
        />

        <Footer/>
      </div>
    )
  }
}

ReactDom.render(<App/>, document.getElementById('app'));
