import React from 'react';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';

import Lefter from './Lefter';
import ShowcaseBenchmarks from './ShowcaseBenchmarks';
import StartBenchmark from './StartBenchmark';
import ListBenchmarks from './ListBenchmarks';

export default class Body extends React.Component {
  // this.props.sessionInfo
  constructor() {
    super();

    this.state = {
      actionPath: "startBenchmark"
    }
  }

  render() {
    return (
      <div style={{backgroundColor: '#EEEEEE', padding: '10px 0 0 0'}}>
        <Grid>
          <Col md={2} style={{padding: '15px 0 0 0'}}>
            <Lefter
              sessionInfo={this.props.sessionInfo}
              onAction={actionPath => {
                try {
                  window.history.pushState(actionPath, "", actionPath);
                } catch (err) {
                  // either non html5 or we are in dev mode loading page from file://
                }
                this.setState({actionPath: actionPath});
              }}
            />
          </Col>

          <Col md={10}>{
            !this.props.sessionInfo.userInfo ? <ShowcaseBenchmarks/> :
              this.state.actionPath === "startBenchmark" ? <StartBenchmark sessionInfo={this.props.sessionInfo}/> :
                this.state.actionPath === "listBenchmarks" ? <ListBenchmarks sessionInfo={this.props.sessionInfo}/> :
                  <Button/>
          }
          </Col>
        </Grid>
      </div>
    );
  }
}
