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
const rouplexLoginUrl = "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/google";

export default class RouplexLogout extends React.Component {
    constructor() {
        super();

        this.state = {
            userEmail: null,
            loggingOut: false,
            failedLogout: false,
            selecting: false,
        };
    }

    handleSignOutClicked() {
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

    handleRouplexSignOutClicked() {
        this.handleSignOutClicked();

        // send whatever payload to server
    }

    handleGoogleSignOutClicked() {
        this.handleSignOutClicked();

        // send whatever payload to server
    }

    handleFacebookSignOutClicked() {
        this.handleSignOutClicked();

        // send whatever payload to server
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
                            onClick={() => this.handleSignOutClicked()}>
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
