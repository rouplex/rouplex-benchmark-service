import React from 'react';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import BootstrapTable from 'react-bootstrap-table/lib/BootstrapTable';
import TableHeaderColumn from 'react-bootstrap-table/lib/TableHeaderColumn';
import WaitingHorizontal from './components/WaitingHorizontal';

const config = require("./Config.js");

const narrowHeaderStyle = {
  fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "10%", minWidth: '150px'
};
const narrowColumnStyle = {
  fontSize: '0.95em', textAlign: 'right', cursor: 'default', width: "10%", minWidth: '150px', fontFamily: 'monospace'
};
const wideHeaderStyle = {
  fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "30%", minWidth: '175px'
};
const wideColumnStyle = {
  fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "30%", minWidth: '175px', fontFamily: 'monospace'
};
const mediumHeaderStyle = {
  fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "20%", minWidth: '175px'
};
const mediumColumnStyle = {
  fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "20%", minWidth: '175px', fontFamily: 'monospace'
};

export default class ListBenchmarks extends React.Component {
  // this.props.sessionInfo
  // this.props.onPathUpdate
  constructor() {
    super();

    this.state = {};
    this.entries = [];

    this.sortOptions = {
      defaultSortName: 'startTime',
      defaultSortOrder: 'desc'
    };
  }

  componentDidMount() {
    this.updateEntries();
    this.intervalId = setInterval((t) => {
      t.updateEntries()
    }, 5000, this);
  }

  componentWillUnmount() {
    clearInterval(this.intervalId);
  }

  handleFailedList(cause) {
    alert("Failed listing TcpEchoBenchmarks. Cause: " + cause);
  }

  handleFailedDescribe(cause) {
    alert("Failed describing TcpEchoBenchmark. Cause: " + cause);
  }

  updateEntry(benchmarkId) {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("tcpEchoBenchmarkUrl.get.response: " + getRequest.responseText);
      try {
        var response = JSON.parse(getRequest.responseText);
      } catch (err) {
        return this.handleFailedDescribe("Non parsable server response. Cause: " + err);
      }

      if (response.exceptionMessage) {
        this.entries.push({
          benchmarkId: benchmarkId,
          startTime: "Unknown",
          connectionsPerSec: "Unknown",
          maxConnections: "Unknown",
          maxThroughput: "Unknown"
        });
        return this.handleFailedDescribe("Server exception. Cause: " + response.exceptionMessage);
      }

      var maxThroughput = response.tcpServerExpectation.maxUploadSpeedInBitsPerSecond > response.tcpServerExpectation.maxDownloadSpeedInBitsPerSecond ?
        response.tcpServerExpectation.maxUploadSpeed : response.tcpServerExpectation.maxDownloadSpeed;

      this.entries.push({
        benchmarkId: benchmarkId,
        startTime: new Date(response.startingTimestamp).toISOString(),
        connectionsPerSec: response.tcpServerExpectation.connectionsPerSecond,
        maxConnections: response.tcpServerExpectation.maxSimultaneousConnections,
        maxThroughput: maxThroughput
      });

      this.setState({entries: this.entries});
    });
    getRequest.addEventListener("error", () => this.handleFailedList());

    getRequest.open("GET", config.tcpEchoBenchmarkUrl + "/" + benchmarkId);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.setRequestHeader('Rouplex-SessionId', this.props.sessionInfo.sessionId);

    console.log("tcpEchoBenchmarkUrl.get.request: " + config.tcpEchoBenchmarkUrl + "/" + benchmarkId);
    getRequest.send();
  }

  updateEntries() {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("tcpEchoBenchmarkUrl.get.response: " + getRequest.responseText);
      try {
        var response = JSON.parse(getRequest.responseText);
      } catch (err) {
        return this.handleFailedList("Non parsable server response. Cause: " + err);
      }

      if (response.exceptionMessage) {
        return this.handleFailedList("Server exception. Cause: " + response.exceptionMessage);
      }

      this.entries = [];
      response.map(function (benchmarkId) {
        //alert("BM: " + benchmarkId);
        this.updateEntry(benchmarkId);
      }, this);
    });
    getRequest.addEventListener("error", () => this.handleFailedList());

    getRequest.open("GET", config.tcpEchoBenchmarkUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    getRequest.setRequestHeader('Rouplex-SessionId', this.props.sessionInfo.sessionId);

    console.log("tcpEchoBenchmarkUrl.get.request: " + config.tcpEchoBenchmarkUrl);
    getRequest.send();
  }

  render() {
    return (
      <Grid style={{padding: "0"}}>
        <Row>
          <Col md={10}>
            <BootstrapTable data={ this.entries } options={ this.sortOptions } striped hover condensed pagination>
              <TableHeaderColumn dataField="benchmarkId" isKey dataSort
                                 dataFormat={(cell) => (<a onClick={(evt) => this.props.onPathUpdate("/benchmark/show#" + evt.target.text)}>{cell}</a>)}
                                 thStyle={wideHeaderStyle} tdStyle={wideColumnStyle}>
                Benchmark Id
              </TableHeaderColumn>
              <TableHeaderColumn dataField="startTime" dataSort
                                 dataFormat={(cell) => (cell ? cell : <WaitingHorizontal/>)}
                                 thStyle={mediumHeaderStyle} tdStyle={mediumColumnStyle}>
                Start Time (UTC)
              </TableHeaderColumn>
              <TableHeaderColumn dataField="connectionsPerSec" dataSort thStyle={narrowHeaderStyle}
                                 tdStyle={narrowColumnStyle}>
                Connections / Sec
              </TableHeaderColumn>
              <TableHeaderColumn dataField="maxConnections" dataSort thStyle={narrowHeaderStyle}
                                 tdStyle={narrowColumnStyle} dataSort>
                Max Connections
              </TableHeaderColumn>
              <TableHeaderColumn dataField="maxThroughput" dataSort thStyle={narrowHeaderStyle}
                                 tdStyle={narrowColumnStyle} dataSort>
                Max Throughput
              </TableHeaderColumn>
            </BootstrapTable>
          </Col>
        </Row>
      </Grid>
    );
  }
}
