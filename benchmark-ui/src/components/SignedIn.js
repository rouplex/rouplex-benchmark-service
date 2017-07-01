import React from 'react';
import DropdownButton from 'react-bootstrap/lib/DropdownButton';
import MenuItem from 'react-bootstrap/lib/MenuItem';
import Checkbox from 'react-bootstrap/lib/Checkbox';
import Button from 'react-bootstrap/lib/Button';

const leftMargin = {margin: '0 0 0 5px'};

export default class SignedIn extends React.Component {
  // this.props.signOutUrl
  // this.props.sessionInfo
  // this.props.mainUrl (optional)
  // this.props.onPreferencesUpdate
  constructor() {
    super();

    this.state = {
      signingOut: false,
      failedSignOut: false,
      selecting: false
    }
  }

  handleSignOutClicked() {
    this.setState({
      signingOut: true,
      selecting: false
    });

    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("signOutUrl.response: " + getRequest.responseText);
      if (this.props.mainUrl) {
        window.location.href = this.props.mainUrl;
      }
    });
    getRequest.addEventListener("error", () => {
      document.cookie = 'Rouplex-SessionId=';
      if (this.props.mainUrl) {
        window.location.href = this.props.mainUrl;
      }
    });

    getRequest.open("GET", this.props.signOutUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.setRequestHeader('Rouplex-SessionId', this.props.sessionInfo.sessionId);

    console.log("signOutUrl.get.request: " + this.props.signOutUrl);
    getRequest.send();
  }

  render() {
    return (
      <DropdownButton id="login" style={leftMargin} key="1"
                      bsStyle={this.state.failedSignOut ? "warning" : "primary"}
                      title={
                        this.state.signingOut
                          ? "Signing Out ..."
                          : this.props.sessionInfo.userInfo.userName + " " + this.props.sessionInfo.userInfo.userIdAtProvider
                      }
                      disabled={this.state.signingOut} open={this.state.selecting}
                      onClick={() => this.setState({selecting: !this.state.selecting})}
                      onToggle={() => null}>
        <MenuItem>
          <Button bsStyle="primary" style={leftMargin} block
                  onClick={() => this.handleSignOutClicked()}>
            Sign Out
          </Button>
        </MenuItem>

        <MenuItem divider/>

        <MenuItem>
          <Checkbox checked={this.props.sessionInfo.userInfo.userPreferences.useUtcTime}
                    onChange={key => this.props.onPreferencesUpdate({useUtcTime: key.target.checked})}>
            &nbsp;Use UTC Time
          </Checkbox>
        </MenuItem>

        <MenuItem>
          <Button bsStyle="default" style={leftMargin} block disabled>
            Profile &nbsp; (coming soon)
          </Button>
        </MenuItem>
      </DropdownButton>
    );
  }
}
