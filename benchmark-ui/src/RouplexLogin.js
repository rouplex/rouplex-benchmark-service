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

const leftMargin = {margin: '0px 0px 0px 5px'};
const googleAuthUrl = "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/google";

export default class RouplexLogin extends React.Component {
    constructor() {
        super();

        this.state = {
            userEmail: null,
            loggingIn: false,
            failedLogin: false,
            selecting: false,
        };
    }

    handleSignInClicked() {
        this.setState({
            loggingIn: true,
            failedLogin: false,
            selecting: false,
        });
    }

    handleFailedSignIn() {
        this.setState({
            loggingIn: false,
            failedLogin: true,
            selecting: false,
        });
    }

    handleRouplexSignIn() {
        this.handleSignInClicked();

        // implement
    }

    handleGoogleSignInClicked() {
        this.handleSignInClicked();

        var getRequest = new XMLHttpRequest();
        getRequest.addEventListener("load", this.handleGoogleAuthResponse);
        getRequest.addEventListener("error", () => {
            this.handleFailedSignIn()
        });
        getRequest.open("GET", googleAuthUrl);
        getRequest.setRequestHeader('Accept', 'application/json');
        getRequest.send();
    }

    handleGoogleAuthResponse() {
        console.log("GoogleAuthResponse: " + this.responseText);
        var userInfo = JSON.parse(this.responseText);
        if (userInfo.redirectUrl) {
            window.location.href = userInfo.redirectUrl;
        } else {
            this.setState({
                userEmail: userInfo.userEmail
            });
        }
    }

    handleFacebookSignIn() {
        this.handleSignInClicked();

        // implement
    }

    render() {
        return (
            <DropdownButton id="login" style={leftMargin} key="1"
                            bsStyle={this.state.failedLogin ? "warning" : "primary"}
                            title={this.state.loggingIn ? "Signing In ..." :
                                                this.state.failedLogin ? "Failed Signing In. Retry" : "Sign In"}
                            disabled={this.state.loggingIn} open={this.state.selecting}
                            onClick={() => this.setState({selecting: !this.state.selecting})}
                            onToggle={() => null}>
                <MenuItem>
                    <FormGroup>
                        <FormControl type="text" id="email" style={leftMargin} placeholder="Email" disabled/>
                        <FormControl type="password" style={leftMargin} placeholder="Password" disabled/>
                        <Button bsStyle="primary" style={leftMargin}
                                onClick={() => this.handleRouplexSignIn()} disabled>
                            Email / Password Sign In1
                        </Button>
                    </FormGroup>
                </MenuItem>

                <MenuItem divider/>

                <MenuItem>
                    <Button bsStyle="primary" style={leftMargin} block
                            onClick={() => this.handleGoogleSignInClicked()}>
                        Google Auth Sign In
                    </Button>
                </MenuItem>

                <MenuItem divider/>

                <MenuItem>
                    <Button bsStyle="primary" style={leftMargin} block disabled
                            onClick={() => this.handleFacebookSignIn()}>
                        Facebook Auth Sign In  &nbsp; (coming soon)
                    </Button>
                </MenuItem>
            </DropdownButton>
        );
    }
}
