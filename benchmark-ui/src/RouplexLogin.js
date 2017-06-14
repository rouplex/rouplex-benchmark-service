import React from 'react';
import ButtonToolbar from 'react-bootstrap/lib/ButtonToolbar';
import Button from 'react-bootstrap/lib/Button';
import ButtonGroup from 'react-bootstrap/lib/ButtonGroup';
import Navbar from 'react-bootstrap/lib/Navbar';
import Nav from 'react-bootstrap/lib/Nav';
import NavDropdown from 'react-bootstrap/lib/NavDropdown';
import NavItem from 'react-bootstrap/lib/NavItem';
import MenuItem from 'react-bootstrap/lib/MenuItem';
import FormGroup from 'react-bootstrap/lib/FormGroup';
import FormControl from 'react-bootstrap/lib/FormControl';
import SplitButton from 'react-bootstrap/lib/SplitButton';
import DropdownButton from 'react-bootstrap/lib/DropdownButton';
import ListGroup from 'react-bootstrap/lib/ListGroup';
import ListGroupItem from 'react-bootstrap/lib/ListGroupItem';
var config = require("./Config.js");

const leftMargin = {margin: '0px 0px 0px 5px'};

export default class RouplexLogin extends React.Component {
  constructor() {
    super();

    this.state = {
      loggingIn: false,
      failedLogin: false,
      selecting: false
    };
  }

  handleSignInClicked() {
    this.setState({
      loggingIn: true,
      failedLogin: false,
      selecting: false
    });
  }

  handleFailedSignIn() {
    this.setState({
      loggingIn: false,
      failedLogin: true,
      selecting: false
    });
  }

  handleRouplexSignInClicked() {
    this.handleSignInClicked();

    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("signInViaRouplexUrl.response: " + getRequest.responseText);
      try {
        var newSessionInfo = JSON.parse(getRequest.responseText)
      } catch (err) {
        alert("Implementation Error " + err);
        this.handleFailedSignIn();
        return;
      }

      if (newSessionInfo.userInfo) { // user is signed in
        this.props.onSessionUpdate(newSessionInfo);
      } else {
        alert("Authentication Error: " + newSessionInfo.exceptionMessage);
        this.handleFailedSignIn();
      }
    });
    getRequest.addEventListener("error", () => this.handleFailedSignIn());

    getRequest.open("GET", config.signInViaRouplexUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.setRequestHeader('Rouplex-Auth-Email', this.email.value);
    getRequest.setRequestHeader('Rouplex-Auth-Password', this.password.value);
    getRequest.send();
  }

  handleGoogleSignInClicked() {
    this.handleSignInClicked();

    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("signInViaGoogleUrl.response: " + getRequest.responseText);
      try {
        var newSessionInfo = JSON.parse(getRequest.responseText)
      } catch (err) {
        alert("Implementation Error " + err);
        return;
      }

      if (newSessionInfo.userInfo) { // user is signed in
        this.props.onSessionUpdate(newSessionInfo);
      } else if (newSessionInfo.redirectUrl) { // user is invited to sign
        window.location.href = newSessionInfo.redirectUrl;
      } else {
        alert("Implementation Error. No UserInfo or redirectUrl present.");
      }
    });
    getRequest.addEventListener("error", () => this.handleFailedSignIn());
    ;
    getRequest.open("GET", config.signInViaGoogleUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.send();
  }

  handleFacebookSignInClicked() {
    this.handleSignInClicked();

    // implement
  }

  render() {
    return (
      <DropdownButton id="login" style={leftMargin} key="1"
                      bsStyle={this.state.failedLogin ? "warning" : "primary"}
                      title={
                        this.state.loggingIn
                          ? "Signing In ..."
                          : this.state.failedLogin
                            ? "Failed Signing In. Retry"
                            : "Sign In"
                      }
                      disabled={this.state.loggingIn} open={this.state.selecting}
                      onClick={() => this.setState({selecting: !this.state.selecting})}
                      onToggle={() => null}>
        <MenuItem>
          <FormGroup>
            <FormControl type="text" id="email"
                         placeholder="Email" inputRef={email => this.email = email}/>
            <FormControl type="password" id="password" style={leftMargin}
                         placeholder="Password" inputRef={password => this.password = password}/>
            <Button bsStyle="primary" style={leftMargin}
                    onClick={() => this.handleRouplexSignInClicked()}>
              Using Email / Password
            </Button>
          </FormGroup>
        </MenuItem>

        <MenuItem>
          <Button bsStyle="primary" block disabled
                  onClick={() => this.handleRouplexSignUpClicked()}>
            Sign Up (coming soon)
          </Button>
        </MenuItem>

        <MenuItem divider/>

        <MenuItem>
          <Button bsStyle="primary" block disabled={!navigator.cookieEnabled}
                  onClick={() => this.handleGoogleSignInClicked()}>
            Using Google Auth {navigator.cookieEnabled ? "" : "(enable cookies!)"}
          </Button>
        </MenuItem>

        <MenuItem divider/>

        <MenuItem>
          <Button bsStyle="primary" block disabled
                  onClick={() => this.handleFacebookSignInClicked()}>
            Using Facebook Auth &nbsp; (coming soon)
          </Button>
        </MenuItem>
      </DropdownButton>
    )
  }
}
