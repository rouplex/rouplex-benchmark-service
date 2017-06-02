import React from 'react';
import Navbar from 'react-bootstrap/lib/Navbar';
import RouplexLogin from './RouplexLogin';
import RouplexLogout from './RouplexLogout';

export default class RouplexHeader extends React.Component {
    constructor() {
        super();

        console.log("QP: " + window.location.search.substr(1));

        this.state = {
            userEmail: null
        };

        setTimeout(this.pollAuthState(), 10);
    }

    pollAuthState() {
        var getRequest = new XMLHttpRequest();
        getRequest.addEventListener("load", () => {
            console.log(getRequest.responseText);
            var userInfo = JSON.parse(getRequest.responseText);
            this.setState({
                userEmail: userInfo.userEmail
            });
        });
        getRequest.addEventListener("error", () => {
            this.setState({
                userEmail: null
            });
        });
        getRequest.open("GET",
            "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/benchmark/auth/google?"
            + window.location.search.substr(1));

        getRequest.setRequestHeader('Accept', 'application/json');
        getRequest.send();
    }

    render() {
        return (
            <Navbar fixedTop collapseOnSelect style={{padding: '10px 10px 10px 10px'}}>
                <Navbar.Header>
                    <Navbar.Brand>
                        <a href="#">Rouplex Demo</a>
                    </Navbar.Brand>
                    <Navbar.Toggle />
                </Navbar.Header>
                <Navbar.Collapse>
                    <Navbar.Form pullRight>
                        {this.state.userEmail ? <RouplexLogout userEmail={this.state.userEmail}/> : <RouplexLogin/>}
                    </Navbar.Form>
                </Navbar.Collapse>
            </Navbar>
        );
    }
}
