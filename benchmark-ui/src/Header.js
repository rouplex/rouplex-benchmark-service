import React from 'react';
import Navbar from 'react-bootstrap/lib/Navbar';
import SessionLogin from './components/SessionLogin';
import SessionLogout from './components/SessionLogout';

var config = require("./Config.js");

export default class Header extends React.Component {
  constructor() {
    super();

    setTimeout(this.updateSession(), 1);
  }

  updateSession() {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("signInUrl.response: " + getRequest.responseText);
      try {
        var sessionInfo = JSON.parse(getRequest.responseText);
      } catch (err) {
      }
      this.props.onSessionUpdate(sessionInfo);
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
      <Navbar collapseOnSelect style={{marginBottom: 6, borderRadius: 0}}>
        <Navbar.Header>
          <Navbar.Brand>
            <a href="#">Rouplex Demo 1.0.0.A</a>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Navbar.Form pullRight style={{padding: 0}}>{
            this.props.sessionInfo.userInfo ?
              <SessionLogout
                mainUrl={config.mainUrl}
                signOutUrl={config.signOutUrl}
                sessionInfo={this.props.sessionInfo}
                onSessionUpdate={sessionInfo => this.props.onSessionUpdate(sessionInfo)}
              />
              :
              <SessionLogin
                signInViaGoogleUrl={config.signInUrl + "?provider=google"}
                signInViaRouplexUrl={config.signInUrl + "?provider=rouplex"}
                onSessionUpdate={sessionInfo => this.props.onSessionUpdate(sessionInfo)}
              />
          }
          </Navbar.Form>
        </Navbar.Collapse>
      </Navbar>
    );
  }
}
