import React from 'react';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import Button from 'react-bootstrap/lib/Button';

import Lefter from './Lefter';
import ShowcaseBenchmarks from './ShowcaseBenchmarks';
import StartBenchmark from './StartBenchmark';
import ListBenchmarks from './ListBenchmarks';

export default class Body extends React.Component {
  // this.props.path
  // this.props.sessionInfo
  // this.props.onPathUpdate

  render() {
    return (
      <div style={{backgroundColor: '#EEEEEE', padding: '15px 0 0 0'}}>
        <Grid>
          <Col md={2} style={{padding: '15px 0 0 0'}}>
            <Lefter
              sessionInfo={this.props.sessionInfo}
              onPathUpdate={this.props.onPathUpdate}
            />
          </Col>

          <Col md={10}>{
            !this.props.sessionInfo.userInfo ? <ShowcaseBenchmarks/> :
              this.props.path === "/benchmark/start" ? <StartBenchmark sessionInfo={this.props.sessionInfo}/> :
                this.props.path === "/benchmark/list" ? <ListBenchmarks sessionInfo={this.props.sessionInfo}/> :
                  <Button bsStyle="primary" block> Unkonwn Path: {this.props.path} </Button>
          }</Col>
        </Grid>
      </div>
    );
  }
}
