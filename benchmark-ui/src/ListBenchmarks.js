import React from 'react';
import Grid from 'react-bootstrap/lib/Grid';
import Col from 'react-bootstrap/lib/Col';
import BootstrapTable from 'react-bootstrap-table/lib/BootstrapTable';
import TableHeaderColumn from 'react-bootstrap-table/lib/TableHeaderColumn';
import WaitingHorizontal from './components/WaitingHorizontal';

var config = require("./Config.js");
var narrowHeaderStyle = { fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "10%", minWidth: '150px'};
var wideHeaderStyle = { fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "30%", minWidth: '175px'};
var narrowColumnStyle = { fontSize: '0.95em', textAlign: 'right', cursor: 'default', width: "10%", minWidth: '150px', fontFamily: 'monospace'};
var wideColumnStyle = { fontSize: '0.95em', textAlign: 'left', cursor: 'default', width: "30%", minWidth: '175px', fontFamily: 'monospace'};

export default class ListBenchmarks extends React.Component {
  constructor() {
    super();

    this.state = {};
    this.entries = [];
  }

  componentDidMount() {
    this.updateEntries();
    //setInterval((t) => {t.updateEntries()}, 3000, this);
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
      console.log("tcpEchoBenchmarkUrl.get.response1: " + getRequest.responseText);
      try {
        var response = JSON.parse(getRequest.responseText);
      } catch (err) {
        return this.handleFailedDescribe("Non parsable server response. Cause: " + err);
      }

      if (response.exceptionMessage) {
        return this.handleFailedDescribe("Server exception. Cause: " + response.exceptionMessage);
      }

      var maxThroughput = response.tcpServerExpectation.maxUploadSpeedInBitsPerSecond > response.tcpServerExpectation.maxDownloadSpeedInBitsPerSecond ?
        response.tcpServerExpectation.maxUploadSpeed :response.tcpServerExpectation.maxDownloadSpeed;

      var benchmarkId = response.benchmarkId.length > 16 ? response.benchmarkId.substring(0, 16) + " ..." : response.benchmarkId;

      this.entries.push({
        benchmarkId: benchmarkId,
        startTime: response.tcpServerExpectation.startAsIsoInstant,
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

    //alert("tcpEchoBenchmarkUrl.get.request: " + config.tcpEchoBenchmarkUrl + "/" + benchmarkId);
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
      response.benchmarkIds.map(function (benchmarkId) {
        //alert("BM: " + benchmarkId);
        this.updateEntry(benchmarkId);
      }, this);
    });
    getRequest.addEventListener("error", () => this.handleFailedList());

    getRequest.open("GET", config.tcpEchoBenchmarkUrl);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);

    //alert("tcpEchoBenchmarkUrl.get.request: " + config.tcpEchoBenchmarkUrl);
    getRequest.send();
  }

  render() {
    return (
      <Grid style={{padding: "0"}}>
        <Col md={10}>
          <BootstrapTable data={ this.entries } striped hover condensed pagination>
            <TableHeaderColumn dataField="benchmarkId" isKey dataSort dataFormat={(cell) => (<a onClick={() => alert("Sdf")}>{cell}</a>)}
                               thStyle={wideHeaderStyle} tdStyle={wideColumnStyle}>
              Benchmark Id
            </TableHeaderColumn>
            <TableHeaderColumn dataField="startTime" dataSort dataFormat={(cell) => (cell ? cell : <WaitingHorizontal/>)}
                               thStyle={wideHeaderStyle} tdStyle={wideColumnStyle}>
              Start Time (UTC)
            </TableHeaderColumn>
            <TableHeaderColumn dataField="connectionsPerSec" dataSort thStyle={narrowHeaderStyle} tdStyle={narrowColumnStyle}>
              Connections / Sec
            </TableHeaderColumn>
            <TableHeaderColumn dataField="maxConnections" dataSort thStyle={narrowHeaderStyle} tdStyle={narrowColumnStyle} dataSort>
              Max Connections
            </TableHeaderColumn>
            <TableHeaderColumn dataField="maxThroughput" dataSort thStyle={narrowHeaderStyle} tdStyle={narrowColumnStyle} dataSort>
              Max Throughput
            </TableHeaderColumn>
          </BootstrapTable>
        </Col>
      </Grid>
    );
  }
}
