import React from 'react';
import DropdownButton from 'react-bootstrap/lib/DropdownButton';
import MenuItem from 'react-bootstrap/lib/MenuItem';
import Button from 'react-bootstrap/lib/Button';

const leftMargin = {margin: '0px 0px 0px 5px'};

export default class SessionLogout extends React.Component {
  // this.props.mainUrl
  // this.props.signOutUrl
  // this.props.sessionInfo
  constructor() {
    super();

    this.state = {
      loggingOut: false,
      failedLogout: false,
      selecting: false
    }
  }

  handleSignOutClicked() {
    this.setState({
      loggingOut: true,
      selecting: false
    });

    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("signOutUrl.response: " + getRequest.responseText);
      window.location.href = this.props.mainUrl;
    });
    getRequest.addEventListener("error", () => {
      document.cookie = 'Rouplex-SessionId=';
      window.location.href = this.props.mainUrl;
    });

    getRequest.open("GET", this.props.signOutUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.setRequestHeader('Rouplex-SessionId', this.props.sessionInfo.sessionId);
    getRequest.send();
  }

  render() {
    return (
      <DropdownButton id="login" style={leftMargin} key="1"
                      bsStyle={this.state.failedLogout ? "warning" : "primary"}
                      title={
                        this.state.loggingOut
                          ? "Signing Out ..."
                          : this.props.sessionInfo.userInfo.userName + " " + this.props.sessionInfo.userInfo.userIdAtProvider
                      }
                      disabled={this.state.loggingOut} open={this.state.selecting}
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
          <Button bsStyle="default" style={leftMargin} block disabled>
            Profile &nbsp; (coming soon)
          </Button>
        </MenuItem>
      </DropdownButton>
    );
  }
}
