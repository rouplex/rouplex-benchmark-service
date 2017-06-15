import React from 'react';
import Button from 'react-bootstrap/lib/Button';

export default class Lefter extends React.Component {
  // this.props.sessionInfo
  // this.props.onAction

  render() {
    return (
      <div>
        <Button block disabled={!this.props.sessionInfo.userInfo} onClick={() => this.props.onAction("startBenchmark")}>
          Start Benchmark »
        </Button>
        <Button block disabled={!this.props.sessionInfo.userInfo} onClick={() => this.props.onAction("listBenchmarks")}>
          List Benchmarks »
        </Button>
      </div>
    );
  }
}
