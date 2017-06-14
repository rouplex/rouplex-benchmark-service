import React from 'react';
import Button from 'react-bootstrap/lib/Button';

export default class Lefter extends React.Component {
  render() {
    return (
      <div>
        <Button block onClick={() => this.props.onAction("startBenchmark")}>Start Benchmark »</Button>
        <Button block onClick={() => this.props.onAction("listBenchmarks")}>List Benchmarks »</Button>
      </div>
    );
  }
}
