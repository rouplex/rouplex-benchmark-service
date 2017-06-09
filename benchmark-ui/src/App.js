import React from 'react';
import 'bootstrap/less/bootstrap.less';
import './styles/custom-styles.css';

import RouplexHeader from './RouplexHeader';
import RouplexBody from './RouplexBody';
import RouplexFooter from './RouplexFooter';

//global.React = React;

class App extends React.Component {
    constructor() {
        super();

        this.state = {
            sessionInfo: {}
        };
    }

    handleSessionUpdate(newSessionInfo) {
        console.log("handlingSessionUpdate: " + newSessionInfo);
        this.setState({
            sessionInfo: newSessionInfo ? newSessionInfo : {}
        })
    }

    render() {
        return (
            <div>
                <RouplexHeader sessionInfo={this.state.sessionInfo}
                               onSessionUpdate={(newSessionInfo) => this.handleSessionUpdate(newSessionInfo)}/>
                <RouplexBody sessionInfo={this.state.sessionInfo}/>
                <RouplexFooter />
            </div>
        )
    }
}

React.render(<App/>, document.getElementById('app'));
