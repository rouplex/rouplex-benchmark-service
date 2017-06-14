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
import Label from 'react-bootstrap/lib/Label';

var config = require("./Config.js");
const leftMargin = {margin: '0px 0px 0px 5px'};

export default class RouplexLogout extends React.Component {
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
      window.location.href = config.mainUrl;
    });
    getRequest.addEventListener("error", () => {
      document.cookie = 'Rouplex-SessionId=';
      window.location.href = config.mainUrl;
    });

    getRequest.open("GET", config.signOutUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    console.log("signOutUrl.request.header.Rouplex-SessionId: " + this.props.sessionInfo.sessionId);
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
    )
  }
}
