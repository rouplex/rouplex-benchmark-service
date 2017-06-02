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
const aroundMargin = {padding: '10px 10px 10px 10px'};
//const rouplexLoginUrl = "https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/login1";
const rouplexLoginUrl = "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/google";

export default class RouplexLogin extends React.Component {
    constructor() {
        super();

        this.state = {
            loggingIn: false,
            failedLogin: false,
            selecting: false,
        };
    }

    getQueryStringValue(key) {
        return decodeURIComponent(window.location.search
            .replace(new RegExp("^(?:.*[&\\?]" + encodeURIComponent(key)
                    .replace(/[\.\+\*]/g, "\\$&") + "(?:\\=([^&]*))?)?.*$", "i"), "$1"));
    }

    handleSignIn() {
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

    handleRedirectToGoogleAuth() {
        console.log("REDIRECT: " + this.responseText);
        var userInfo = JSON.parse(this.responseText);
        window.location.href = userInfo.redirectUrl;
    }

    handleUserProfile() {
        console.log(this.responseText);
        window.location.href = this.responseText;
    }

    handleRouplexSignIn() {
        //this.handleSignIn();

        var getRequest = new XMLHttpRequest();
        getRequest.addEventListener("load", this.handleUserProfile);
        getRequest.addEventListener("error", () => {
            this.handleFailedSignIn()
        });
        getRequest.open("GET", "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/google?code=4/g3gSmKrfPUbwY9fI4FZHs0epZnnyEPLRETJ6qdwz1PA&authuser=0&session_state=e1c3e648c1e12096a0fad93c5403603c1fb316e4..a943&prompt=consent");
        getRequest.setRequestHeader('Accept', 'application/json');
        getRequest.send();
    }

    handleGoogleSignIn() {
        this.handleSignIn();

        var getRequest = new XMLHttpRequest();
        getRequest.addEventListener("load", this.handleRedirectToGoogleAuth);
        getRequest.addEventListener("error", () => {
            this.handleFailedSignIn()
        });
        getRequest.open("GET", rouplexLoginUrl);
        getRequest.setRequestHeader('Accept', 'application/json');
        getRequest.send();
    }

    handleFacebookSignIn() {
        this.handleSignIn();

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
                        <FormControl type="text" id="email" style={leftMargin} placeholder="Email"/>
                        <FormControl type="password" style={leftMargin} placeholder="Password"/>
                        <Button bsStyle="primary" style={leftMargin}
                                onClick={() => this.handleRouplexSignIn()}>
                            Email / Password Sign In
                        </Button>
                    </FormGroup>
                </MenuItem>

                <MenuItem divider/>

                <MenuItem>
                    <Button bsStyle="primary" style={leftMargin} block
                            onClick={() => this.handleGoogleSignIn()}>
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
