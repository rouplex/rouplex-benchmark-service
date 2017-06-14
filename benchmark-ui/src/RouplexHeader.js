import React from 'react';
import Navbar from 'react-bootstrap/lib/Navbar';
//import Config from "./Config.js";
import RouplexLogin from './RouplexLogin';
import RouplexLogout from './RouplexLogout';

var config = require("./Config.js");

export default class RouplexHeader extends React.Component {
  constructor() {
    super();

    console.log("index.html.qp: " + window.location.search.substr(1));

    setTimeout(this.updateSession(), 1);
  }

  updateSession() {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("signInUrl.response: " + getRequest.responseText);
      try {
        var newSessionInfo = JSON.parse(getRequest.responseText)
      } catch (err) {
      }
      this.props.onSessionUpdate(newSessionInfo);
    });
    getRequest.addEventListener("error", () => this.props.onSessionUpdate(null));

    // send all the query string (empty on initial requests) along with occasional sessionId cookie
    var params = window.location.search.substr(1);

    getRequest.open("GET", params && params.length > 0 ? config.signInUrl + "?" + params : config.signInUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.send();
  }

  render() {
    return (
      <Navbar fixedTop collapseOnSelect style={{padding: '10px 10px 10px 10px'}}>
        <Navbar.Header>
          <Navbar.Brand>
            <a href="#">Rouplex Demo 1.0.0.A</a>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Navbar.Form pullRight> {
            this.props.sessionInfo.userInfo
              ? <RouplexLogout sessionInfo={this.props.sessionInfo}
                               onSessionUpdate={(newSessionInfo) => this.props.onSessionUpdate(newSessionInfo)}/>
              : <RouplexLogin onSessionUpdate={(newSessionInfo) => this.props.onSessionUpdate(newSessionInfo)}/>
          }
          </Navbar.Form>
        </Navbar.Collapse>
      </Navbar>
    );
  }
}
