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

const leftMargin = {margin: '0px 0px 0px 5px'};
const aroundMargin = {padding: '10px 10px 10px 10px'};
//const rouplexLoginUrl = "https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/login1";
const rouplexLoginUrl = "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/google";

export default class RouplexLogout extends React.Component {
    constructor() {
        super();

        this.state = {
            loggingOut: false,
            failedLogout: false,
            selecting: false,
        };
    }

    handleSignOut() {
        this.setState({
            loggingOut: true,
            selecting: false,
        });
    }

    handleFailedSignOut() {
        this.setState({
            loggingOut: false,
            failedLogout: true,
            selecting: false,
        });
    }

    handleRouplexSignOut() {
        this.handleSignOut();

        // send request to invalidate token

        //var getRequest = new XMLHttpRequest();
        //getRequest.addEventListener("load", this.handleUserProfile);
        //getRequest.addEventListener("error", () => {
        //    this.handleFailedSignIn()
        //});
        //getRequest.open("GET", "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/google?code=4/g3gSmKrfPUbwY9fI4FZHs0epZnnyEPLRETJ6qdwz1PA&authuser=0&session_state=e1c3e648c1e12096a0fad93c5403603c1fb316e4..a943&prompt=consent");
        //getRequest.send();
    }

    handleGoogleSignOut() {
        this.handleSignOut();

        // whatever
    }

    handleFacebookSignOut() {
        this.handleSignOut();

        // implement
    }

    render() {
        return (
            <DropdownButton id="login" style={leftMargin} key="1"
                            bsStyle={this.state.failedLogout ? "warning" : "primary"}
                            title={this.state.loggingOut ? "Signing Out ..." : this.props.userEmail}
                            disabled={this.state.loggingOut} open={this.state.selecting}
                            onClick={() => this.setState({selecting: !this.state.selecting})}
                            onToggle={() => null}>
                <MenuItem>
                    <Button bsStyle="primary" style={leftMargin} block disabled
                            onClick={() => this.handleSignOut()}>
                        Sign Out &nbsp; (coming soon)
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
