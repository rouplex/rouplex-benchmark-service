import React from 'react';
import Button from 'react-bootstrap/lib/Button';
import MenuItem from 'react-bootstrap/lib/MenuItem';
import FormGroup from 'react-bootstrap/lib/FormGroup';
import FormControl from 'react-bootstrap/lib/FormControl';
import DropdownButton from 'react-bootstrap/lib/DropdownButton';

const leftMargin = {margin: '0px 0px 0px 5px'};

export default class SignIn extends React.Component {
  // this.props.path
  // this.props.getSessionInfoUrl
  // this.props.signInUsingBasicAuthUrl
  // this.props.startSignInUsingGoogleOauth2Url
  // this.props.finishSignInUsingGoogleOauth2Url
  // this.props.onPathUpdate
  // this.props.onSessionUpdate
  constructor() {
    super();

    this.state = {
      signingIn: false,
      failedSignIn: false,
      selecting: false
    };
  }

  componentDidMount() {
    var state = this.getParameterByName("state"); // google's query param for now

    if (!state) {           // not in a callback
      this.updateSession(); // if cookies are enabled, we may learn that we are already in session
    } else {                // in a callback url, right now we only have google
      this.finishSignInUsingGoogleOauth2(state);
    }
  }

  getParameterByName(name, url) { // copied off internet
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
      results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
  }

  handleSignInClicked() {
    this.setState({
      signingIn: true,
      failedSignIn: false,
      selecting: false
    });
  }

  handleFailedSignIn(cause) {
    alert("Failed sign in. Cause: " + cause);

    this.setState({
      signingIn: false,
      failedSignIn: true,
      selecting: false
    });

    return false;
  }

  handleSignInResponse(responseText) {
    try {
      var response = JSON.parse(responseText);
    } catch (err) {
      return this.handleFailedSignIn("Non parsable server response. Cause: " + err);
    }

    if (response.exceptionMessage) { // user failed sign in
      return this.handleFailedSignIn("Server exception. Cause: " + response.exceptionMessage);
    }

    if (!response.sessionInfo) { // user is signed in
      return this.handleFailedSignIn("Malformed server response. Cause: missing sessionInfo");
    }

    var sessionInfo = response.sessionInfo;
    if (!sessionInfo.userInfo) {
      return false;
    }

    this.props.onSessionUpdate(sessionInfo);
    return true;
  }

  updateSession() {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("getSessionInfo.get.response: " + getRequest.responseText);
      if (!this.handleSignInResponse(getRequest.responseText)) {
        this.props.onSessionUpdate({})
      }
    });
    getRequest.addEventListener("error", () => this.props.onSessionUpdate({}));

    getRequest.open("GET", this.props.getSessionInfoUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);

    console.log("getSessionInfo.get.request: " + this.props.getSessionInfoUrl);
    getRequest.send();
  }

  handleRouplexSignInClicked() {
    this.handleSignInClicked();

    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("signInUsingBasicAuthUrl.get.response: " + getRequest.responseText);
      this.handleSignInResponse(getRequest.responseText);
    });
    getRequest.addEventListener("error", () => this.handleFailedSignIn("Communication error"));

    getRequest.open("GET", this.props.signInUsingBasicAuthUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.setRequestHeader('Rouplex-Auth-UserId', this.email.value);
    getRequest.setRequestHeader('Rouplex-Auth-Password', this.password.value);

    console.log("signInUsingBasicAuthUrl.get.request: " + this.props.signInUsingBasicAuthUrl);
    //alert("signInUsingBasicAuthUrl.get.request: " + this.props.signInUsingBasicAuthUrl);
    getRequest.send();
  }

  handleGoogleSignInClicked() {
    this.handleSignInClicked();
    this.startSignInUsingGoogleOauth2Url();
  }

  startSignInUsingGoogleOauth2Url() {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("startSignInUsingGoogleOauth2Url.get.response: " + getRequest.responseText);
      try {
        var response = JSON.parse(getRequest.responseText);
      } catch (err) {
        return this.handleFailedSignIn("Non parsable server response. Cause: " + err);
      }

      if (response.exceptionMessage) { // user failed sign in
        return this.handleFailedSignIn("Server exception. Cause: " + response.exceptionMessage);
      }

      if (!response.redirectUrl) { // user is invited to sign in
        return this.handleFailedSignIn("Malformed server response. Cause: missing redirectUrl");
      }

      window.location.href = response.redirectUrl;
    });
    getRequest.addEventListener("error", () => this.handleFailedSignIn());

    getRequest.open("GET", this.props.startSignInUsingGoogleOauth2Url + "?state=" + this.props.path);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);

    //alert("signInUsingGoogleOauth2Url.get.request: " + this.props.startSignInUsingGoogleOauth2Url + "?redirect=" + window.location.pathname);
    getRequest.send();
  }

  finishSignInUsingGoogleOauth2(state) {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("finishSignInUsingGoogleOauth2.get.response: " + getRequest.responseText);
      if (this.handleSignInResponse(getRequest.responseText)) {
        var startIndex = state.indexOf(";") + ";".length;
        var finishIndex = state.indexOf(";", startIndex);
        var path = state.substring(startIndex, finishIndex);
        this.props.onPathUpdate(path);
      }
    });
    getRequest.addEventListener("error", () => this.props.onSessionUpdate({}));

    getRequest.open("GET", this.props.finishSignInUsingGoogleOauth2Url + window.location.search);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);

    console.log("finishSignInUsingGoogleOauth2.get.request: " + this.props.finishSignInUsingGoogleOauth2Url + window.location.search);
    getRequest.send();
  }

  handleFacebookSignInClicked() {
    this.handleSignInClicked();

    // implement
  }

  render() {
    return (
      <DropdownButton id="login" style={leftMargin} key="1"
                      bsStyle={this.state.failedSignIn ? "warning" : "primary"}
                      title={
                        this.state.signingIn
                          ? "Signing In ..."
                          : this.state.failedSignIn
                            ? "Failed Signing In. Retry"
                            : "Sign In"
                      }
                      disabled={this.state.signingIn} open={this.state.selecting}
                      onClick={() => this.setState({selecting: !this.state.selecting})}
                      onToggle={() => null}>

        <MenuItem>
          <FormGroup>
            <FormControl
              type="text" id="email"
              placeholder="Email" inputRef={email => this.email = email}
            />
            <FormControl
              type="password" id="password" style={leftMargin}
              placeholder="Password" inputRef={password => this.password = password}
            />
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
    );
  }
}
