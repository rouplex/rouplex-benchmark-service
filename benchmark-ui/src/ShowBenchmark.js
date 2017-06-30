import React from 'react';
import Panel from 'react-bootstrap/lib/Panel';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import Checkbox from 'react-bootstrap/lib/Checkbox';
import Alert from 'react-bootstrap/lib/Alert';
import Tooltip from 'react-bootstrap/lib/Tooltip';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';

import LabeledValue from './components/LabeledValue';
import LabeledValue2 from './components/LabeledValue2';
import WaitingHorizontal from './components/WaitingHorizontal';
import CopyToClipboard from 'react-copy-to-clipboard';

var config = require("./Config.js");

export default class ShowBenchmark extends React.Component {
  // this.props.benchmarkId
  constructor() {
    super();

    this.state = {
      response: null,
      exception: null
    }
  }
  componentDidMount() {
    this.update();
    this.intervalId = setInterval((t) => {
      t.update()
    }, 5000, this);
  }

  componentWillUnmount() {
    clearInterval(this.intervalId);
  }

  update() {
    var getRequest = new XMLHttpRequest();
    getRequest.addEventListener("load", () => {
      console.log("describeTcpEchoBenchmarkUrl.get.response: " + getRequest.responseText);
      try {
        var response = JSON.parse(getRequest.responseText);
      } catch (err) {
        return this.setState({exception: "Non parsable server response. Details: " + err});
      }

      if (response.exceptionMessage) {
        return this.setState({exception: "Server error. Details: " + response.exceptionMessage});
      }

      this.setState({response: response, exception: null});
    });
    getRequest.addEventListener("error", () => this.setState({exception: "Communication error"}));

    getRequest.open("GET", config.tcpEchoBenchmarkUrl + "/" + this.props.benchmarkId);
    getRequest.setRequestHeader('Accept', 'application/json');
    getRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);

    //alert("describeTcpEchoBenchmarkUrl.get.request: " + config.tcpEchoBenchmarkUrl + "/" + benchmarkId);
    getRequest.send();
  }

  renderBenchmarkParams(response) {
    return (
      <Row>
        <Col md={4}>
          <Row>
            <Col md={3}>
              <Checkbox style={{margin: '0 0 0 8px'}} checked={response.ssl} disabled={true}>
                &nbsp; SSL
              </Checkbox>
            </Col>
            <Col style={{textAlign: 'right'}} md={4}>
              <label>Provider</label>
            </Col>
            <Col md={5}>
              {response.provider}
            </Col>
          </Row>

          <Panel header="Server">
            <LabeledValue label="Geo Location" value={response.serverGeoLocation} colSpans={[7,5]}/>
            <LabeledValue label="Host Type" value={response.serverHostType} colSpans={[7,5]}/>
            <LabeledValue label="Backlog (int)" value={response.backlog} colSpans={[7,5]}/>
            <LabeledValue label="Echo Ratio (x:y)" value={response.echoRatio} colSpans={[7,5]}/>
          </Panel>

          <Panel header="Extra Parameters">
            <LabeledValue label="Aws Key Name" value={response.keyName} colSpans={[7,5]}/>
            <LabeledValue label="Socket Send Buffer Size" value={response.socketSendBufferSize}
                          colSpans={[7,5]}/>
            <LabeledValue label="Socket Receive Buffer Size" value={response.socketReceiveBufferSize}
                          colSpans={[7,5]}/>
          </Panel>
        </Col>

        <Col md={6}>
          <LabeledValue label="Benchmark Id" value={response.benchmarkId} colSpans={[4,8]}/>

          <Panel header="Clients">
            <LabeledValue label="Geo Location" value={response.clientsGeoLocation} colSpans={[5,7]}/>
            <LabeledValue label="Host Type" value={response.clientsHostType} colSpans={[5,7]}/>
            <LabeledValue2 label="Clients (count)"
                           label1="Per Host" value1={response.clientsPerHost}
                           label2="Total" value2={response.clientCount}/>
            <LabeledValue2 label="Payload (bytes)" value1={response.minPayloadSize}
                           value2={response.maxPayloadSize}/>
            <LabeledValue2 label="Delay Between Sends (millis)" value1={response.minDelayMillisBetweenSends}
                           value2={response.maxDelayMillisBetweenSends}/>
            <LabeledValue2 label="Delay Creating Client (millis)"
                           value1={response.minDelayMillisBeforeCreatingClient}
                           value2={response.maxDelayMillisBeforeCreatingClient}/>
            <LabeledValue2 label="Client Lifespan (millis)" value1={response.minClientLifeMillis}
                           value2={response.maxClientLifeMillis}/>
          </Panel>
        </Col>
      </Row>
    );
  }

  renderServerExpectation(tcpServerExpectation) {
    return (
      <Row>
        <Col md={10}>
          <Panel header="Server Expectation">
            <LabeledValue label="Ramp Up" value={tcpServerExpectation.rampUpInMillis + " millis"} colSpans={[3,3]}/>
            <LabeledValue label="Connection Rate" value={tcpServerExpectation.connectionsPerSecond + " per sec"}
                          colSpans={[3,3]}/>
            <LabeledValue label="Max Connections" value={tcpServerExpectation.maxSimultaneousConnections}
                          colSpans={[3,3]}/>
            <LabeledValue label="Max Upload Speed" value={tcpServerExpectation.maxUploadSpeed} colSpans={[3,3]}/>
            <LabeledValue label="Max Download Speed" value={tcpServerExpectation.maxDownloadSpeed} colSpans={[3,3]}/>
            <LabeledValue label="Finish Time" value={tcpServerExpectation.finishAsIsoInstant} colSpans={[3,3]}/>
          </Panel>
        </Col>
      </Row>
    );
  }

  renderJconsoleJmxLink(response) {
    return (
      <Row>
        <Col md={10}>
          <Panel header="Jconsole Jmx Link">
            <Row style={{margin: "0 0 15px 0"}}>
              {response.jconsoleJmxLink
                ?
                <CopyToClipboard text={response.jconsoleJmxLink}>
                  <OverlayTrigger placement="bottom"
                                  overlay={(<Tooltip id="tooltip">Click to copy to clipboard</Tooltip>)}>
                    <a style={{cursor: "pointer"}}>{response.jconsoleJmxLink}</a>
                  </OverlayTrigger>
                </CopyToClipboard>
                :
                <WaitingHorizontal/>
              }
            </Row>
          </Panel>
        </Col>
      </Row>
    );
  }

  renderException(exception) {
    return (
      <Row>
        <Col md={10}>
          <Alert bsStyle="warning">
            <strong>Could not describe benchmark.</strong>&nbsp;{exception}
          </Alert>
        </Col>
      </Row>
    );
  }

  render() {
    return (
      <Grid style={{padding: '0'}}>
        {this.state.response ? this.renderBenchmarkParams(this.state.response) : ""}
        {this.state.response ? this.renderServerExpectation(this.state.response.tcpServerExpectation) : ""}
        {this.state.response ? this.renderJconsoleJmxLink(this.state.response) : ""}
        {this.state.exception ? this.renderException(this.state.exception) : ""}
      </Grid>
    );
  }
}
