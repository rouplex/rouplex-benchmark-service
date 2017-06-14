import React from 'react';
import Button from 'react-bootstrap/lib/Button';
import Grid from 'react-bootstrap/lib/Grid';
import Jumbotron from 'react-bootstrap/lib/Jumbotron';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import LearnMore from './LearnMore';
import TcpBenchmarkStart from './TcpBenchmarkStart';

export default class RouplexBody extends React.Component {
  render() {
    return (
      <div style={{backgroundColor: '#EEEEEE', padding: '10px 0 0 0'}}>
        {this.props.actionPath === "benchmarkStart" ? <TcpBenchmarkStart /> : <Button />}
      </div>
    );
  }
}
