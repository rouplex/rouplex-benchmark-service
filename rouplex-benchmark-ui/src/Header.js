import React from 'react';
import Navbar from 'react-bootstrap/lib/Navbar';
import SignIn from './components/SignIn';
import SignedIn from './components/SignedIn';

var config = require("./Config.js");

export default class Header extends React.Component {
  // this.props.path
  // this.props.sessionInfo
  // this.props.onPathUpdate
  // this.props.onSessionUpdate

  handlePreferencesUpdate(newPreferences) {
    this.props.sessionInfo.userInfo.userPreferences = newPreferences;
    // post new preferences to server
    this.props.onSessionUpdate(this.props.sessionInfo);
  }

  render() {
    return (
      <Navbar collapseOnSelect style={{marginBottom: 6, borderRadius: 0}}>
        <Navbar.Header>
          <Navbar.Brand>
            <a href="#">Rouplex Demo {config.version}</a>
          </Navbar.Brand>
          <Navbar.Toggle />
        </Navbar.Header>
        <Navbar.Collapse>
          <Navbar.Form pullRight style={{padding: 0}}>{
            !this.props.sessionInfo.userInfo ?
              <SignIn
                path={this.props.path}
                getSessionInfoUrl={config.getSessionInfoUrl}
                signInUsingBasicAuthUrl={config.signInUsingBasicAuthUrl}
                startSignInUsingGoogleOauth2Url={config.startSignInUsingGoogleOauth2Url}
                finishSignInUsingGoogleOauth2Url={config.finishSignInUsingGoogleOauth2Url}
                onPathUpdate={path => this.props.onPathUpdate(path)}
                onSessionUpdate={sessionInfo => this.props.onSessionUpdate(sessionInfo)}
              />
              :
              <SignedIn
                mainUrl={config.mainUrl}
                signOutUrl={config.signOutUrl}
                sessionInfo={this.props.sessionInfo}
                onPreferencesUpdate={newPreferences => this.handlePreferencesUpdate(newPreferences)}
              />
          }
          </Navbar.Form>
        </Navbar.Collapse>
      </Navbar>
    );
  }
}
