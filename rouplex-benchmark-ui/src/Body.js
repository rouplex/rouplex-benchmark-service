import React from 'react';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import Button from 'react-bootstrap/lib/Button';

import Lefter from './Lefter';
import ShowcaseBenchmarks from './ShowcaseBenchmarks';
import StartBenchmark from './StartBenchmark';
import ListBenchmarks from './ListBenchmarks';
import ShowBenchmark from './ShowBenchmark';

export default class Body extends React.Component {
  // this.props.path
  // this.props.sessionInfo
  // this.props.onPathUpdate

  renderBody() {
    if (!this.props.sessionInfo.userInfo) {
      return <ShowcaseBenchmarks/>
    }

    switch (this.props.path) {
      case "/benchmark/start":
        return <StartBenchmark sessionInfo={this.props.sessionInfo} onPathUpdate={this.props.onPathUpdate}/>;
      case "/benchmark/list":
        return <ListBenchmarks sessionInfo={this.props.sessionInfo} onPathUpdate={this.props.onPathUpdate}/>;
    }

    var res = this.props.path.split("#");
    switch (res[0]) {
      case "/benchmark/show":
        return <ShowBenchmark sessionInfo={this.props.sessionInfo} benchmarkId={res[1]}/>;
    }

    return <Button bsStyle="primary" block> Unkonwn Path: {this.props.path}</Button>
  }

  render() {
    return (
      <div style={{backgroundColor: '#EEEEEE', padding: '15px 0 0 0'}}>
        <Grid>
          <Col md={2} style={{padding: "0"}}>
            <Lefter
              sessionInfo={this.props.sessionInfo}
              onPathUpdate={this.props.onPathUpdate}
            />
          </Col>

          <Col md={10}>{
            this.renderBody()
          }</Col>
        </Grid>
      </div>
    );
  }
}
