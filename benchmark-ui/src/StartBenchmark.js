import React from 'react';
import Panel from 'react-bootstrap/lib/Panel';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import Button from 'react-bootstrap/lib/Button';

import NioProviderSelector from './components/NioProviderSelector';
import DropdownSelector from './components/DropdownSelector';
import RangeSelector from './components/RangeSelector';
import ValueSelector from './components/ValueSelector';

var config = require("./Config.js");
var validator = require("./components/Validator.js");

export default class StartBenchmark extends React.Component {
  // this.props.sessionInfo
  constructor() {
    super();

    this.state = {
      ssl: false,
      provider: null,
      serverGeoLocation: null,
      serverHostType: null,
      backlog: null,
      echoRatio: null,
      keyName: null,
      socketSendBufferSize: null,
      socketReceiveBufferSize: null,
      benchmarkRequestId: null,
      clientsGeoLocation: null,
      clientsHostType: null,
      clientCount: null,
      clientsPerHost: null,
      payloadSize: {},
      delayMillisBetweenSends: {},
      delayMillisBeforeCreatingClient: {},
      clientLifeMillis: {},

      pendingSubmission: false,
      failedSubmission: false,
      failedValidation: false
    }
  }

  buildRequestBody() {
    var echoRatioQuoted = this.quote(this.state.echoRatio);
    var benchmarkRequestIdQuoted = this.quote(this.state.benchmarkRequestId);
    var keyNameQuoted = this.quote(this.state.keyName);

    var socketSendBufferSize = this.parseIntValue("Socket Send Buffer Size", this.state.socketSendBufferSize, 0);
    var socketReceiveBufferSize = this.parseIntValue("Socket Receive Buffer Size", this.state.socketReceiveBufferSize, 0);
    var backlog = this.parseIntValue("Backlog", this.state.backlog, 0);
    var clientCount = this.parseIntValue("Clients", this.state.clientCount);
    var clientsPerHost = this.parseIntValue("Clients", this.state.clientsPerHost);
    var minPayloadSize = this.parseIntValue("Payload", this.state.payloadSize.min);
    var maxPayloadSize = this.parseIntValue("Payload", this.state.payloadSize.max);
    var minDelayMillisBetweenSends = this.parseIntValue("Delay Between Sends", this.state.delayMillisBetweenSends.min);
    var maxDelayMillisBetweenSends = this.parseIntValue("Delay Between Sends", this.state.delayMillisBetweenSends.max);
    var minDelayMillisBeforeCreatingClient = this.parseIntValue("Delay Creating Client", this.state.delayMillisBeforeCreatingClient.min);
    var maxDelayMillisBeforeCreatingClient = this.parseIntValue("Delay Creating Client", this.state.delayMillisBeforeCreatingClient.max);
    var minClientLifeMillis = this.parseIntValue("Client Lifespan", this.state.clientLifeMillis.min);
    var maxClientLifeMillis = this.parseIntValue("Client Lifespan", this.state.clientLifeMillis.max);

    if (socketSendBufferSize < 0) {
      throw "Socket Send Buffer Size must be non-negative"
    }

    if (socketReceiveBufferSize < 0) {
      throw "Socket Receive Buffer Size must be non-negative"
    }

    if (backlog < 0) {
      throw "Backlog must be non-negative"
    }

    if (clientCount <= 0) {
      throw "Total Clients must be positive"
    }

    if (clientsPerHost <= 0) {
      throw "Clients Per Host must be positive"
    }

    if (minPayloadSize <= 0) {
      throw "Minimum Payload Size must be positive"
    }
    if (maxPayloadSize <= minPayloadSize) {
      throw "Maximum Payload Size must be greater than Minimum Payload Size"
    }

    if (minDelayMillisBetweenSends <= 0) {
      throw "Minimum Delay Between Sends must be positive"
    }
    if (maxDelayMillisBetweenSends <= minDelayMillisBetweenSends) {
      throw "Maximum Delay Between Sends must be greater than Minimum Delay Between Sends"
    }

    if (minDelayMillisBeforeCreatingClient < 0) {
      throw "Minimum Delay Before Creating Client must be non-negative"
    }
    if (maxDelayMillisBeforeCreatingClient <= minDelayMillisBeforeCreatingClient) {
      throw "Maximum Delay Before Creating Client must be greater than Minimum Delay Before Creating Client"
    }

    if (minClientLifeMillis <= 0) {
      throw "Minimum Client Life must be positive"
    }
    if (maxClientLifeMillis <= minClientLifeMillis) {
      throw "Maximum Client Life must be greater than Minimum Client Life"
    }

    var requestBody =
      '{\n' +
      '  "echoRatio" : ' + echoRatioQuoted + ',\n' +
      '  "benchmarkRequestId" : ' + benchmarkRequestIdQuoted + ',\n' +
      '  "serverHostType" : "' + this.state.serverHostType + '",\n' +
      '  "serverGeoLocation" : "' + this.state.serverGeoLocation + '",\n' +
      '  "clientsHostType" : "' + this.state.clientsHostType + '",\n' +
      '  "clientsGeoLocation" : "' + this.state.clientsGeoLocation + '",\n' +
      '  "imageId" : null,\n' +
      '  "keyName" : ' + keyNameQuoted + ',\n' +
      '  "tcpMemoryAsPercentOfTotal" : 0,\n' +
      '  "provider" : "' + this.state.provider + '",\n' +
      '  "port" : 8888,\n' +
      '  "ssl" : "' + this.state.ssl + '",\n' +
      '  "socketSendBufferSize" : ' + socketSendBufferSize + ',\n' +
      '  "socketReceiveBufferSize" : ' + socketReceiveBufferSize + ',\n' +
      '  "backlog" : ' + backlog + ',\n' +
      '  "clientCount" : ' + clientCount + ',\n' +
      '  "clientsPerHost" : ' + clientsPerHost + ',\n' +
      '  "minPayloadSize" : ' + minPayloadSize + ',\n' +
      '  "maxPayloadSize" : ' + maxPayloadSize + ',\n' +
      '  "minDelayMillisBetweenSends" : ' + minDelayMillisBetweenSends + ',\n' +
      '  "maxDelayMillisBetweenSends" : ' + maxDelayMillisBetweenSends + ',\n' +
      '  "minDelayMillisBeforeCreatingClient" : ' + minDelayMillisBeforeCreatingClient + ',\n' +
      '  "maxDelayMillisBeforeCreatingClient" : ' + maxDelayMillisBeforeCreatingClient + ',\n' +
      '  "minClientLifeMillis" : ' + minClientLifeMillis + ',\n' +
      '  "maxClientLifeMillis" : ' + maxClientLifeMillis + '\n' +
      '}\n';

    return requestBody;
  }

  postRequest(requestBody) {
    var postRequest = new XMLHttpRequest();
    postRequest.addEventListener("load", () => {
      console.log("startTcpBenchmark.response: " + postRequest.responseText);
      alert("startTcpBenchmark.response: " + postRequest.responseText);
      this.setState({pendingSubmission: false});
      try {
        // parse response
        this.setState({failedSubmission: false});
      } catch (err) {
        this.setState({failedSubmission: true});
        return;
      }
    });

    postRequest.addEventListener("error", () => {
      console.log("startTcpBenchmark.error: " + postRequest.responseText);
      alert("startTcpBenchmark.error: " + postRequest.responseText);
      this.setState({pendingSubmission: false});
      this.setState({failedSubmission: true});
    });

    postRequest.open("POST", config.startTcpBenchmarkUrl);
    postRequest.setRequestHeader('Content-Type', 'application/json');
    postRequest.setRequestHeader('Accept', 'application/json');
    postRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    postRequest.setRequestHeader('Rouplex-SessionId', this.props.sessionInfo.sessionId);

    console.log("startTcpBenchmark.request: " + requestBody);
    postRequest.send(requestBody);
  }

  handleStartBenchmarkClicked() {
    this.setState({pendingSubmission: true});

    try {
      var requestBody = this.buildRequestBody();
      this.setState({failedValidation: false});
    }
    catch (err) {
      alert("Error starting benchmark. Cause: " + err);
      this.setState({failedValidation: true, pendingSubmission: false});
      return;
    }

    try {
      this.postRequest(requestBody);
    }
    catch (err) {
      alert("Error starting benchmark. Cause: " + err);
      this.setState({failedSubmission: true, pendingSubmission: false});
    }
  }

  parseIntValue(humanName, stringValue, defaultValue) {
    if (validator.isUndefinedNullOrEmpty(stringValue)) {
      if (typeof defaultValue === 'undefined') {
        throw humanName + " has no value";
      }

      return defaultValue;
    }

    if (!validator.isNumeric(stringValue)) {
      throw humanName + " is not a number";
    }

    return parseInt(stringValue);
  }

  quote(stringValue) {
    return validator.isUndefinedNullOrEmpty(stringValue) ? null : '"' + stringValue + '"';
  }

  validateValue(value, range) {
    return validator.validateIntValueWithinRange(value, range,
      {validateSubmittable: this.state.failedValidation, omitSuccessEffect: true});
  }

  validateRange(value, range) {
    return validator.validateIntRangeWithinRange(value, range,
      {validateSubmittable: this.state.failedValidation, omitSuccessEffect: true});
  }

  render() {
    return (
      <Grid style={{padding: '0'}}>
        <Col md={5}>
          <NioProviderSelector
            nioProviders={config.nioProviders}
            onSslChange={value => this.state.ssl = value}
            onNioProviderChange={value => this.state.provider = value}
          />

          <Panel header="Server">
            <DropdownSelector
              label="Geo Location" colSpans={[5,7]} options={config.ec2Regions}
              onChange={value => this.setState({serverGeoLocation: value})}
            />

            <DropdownSelector
              label="Host Type" colSpans={[5,7]} options={config.ec2InstanceTypes}
              onChange={value => this.setState({serverHostType: value})}
            />

            <ValueSelector
              label="Backlog (int)" colSpans={[5,7]} placeholder="Optional, defaults to system's"
              onValidate={value => this.validateValue(value, {min: 0, optional: true})}
              onChange={value => this.state.backlog = value}
            />

            <ValueSelector
              label="Echo Ratio (x:y)" colSpans={[5,7]} placeholder="Optional, defaults to 1:1 (n/a yet)"
              onChange={value => this.state.echoRatio = value}
            />
          </Panel>

          <Panel header="Extra Parameters">
            <ValueSelector
              label="Aws Key Name" colSpans={[7,5]} placeholder="Optional, for ssh host access"
              onChange={value => this.state.keyName = value}
            />

            <ValueSelector
              label="Socket Send Buffer Size (kb)" colSpans={[7, 5]} placeholder="Optional, defaults to system's"
              onValidate={value => this.validateValue(value, {min: 0, optional: true})}
              onChange={value => this.state.socketSendBufferSize = value}
            />

            <ValueSelector
              label="Socket Receive Buffer Size (kb)" colSpans={[7,5]} placeholder="Optional, defaults to system's"
              onValidate={value => this.validateValue(value, {min: 0, optional: true})}
              onChange={value => this.state.socketReceiveBufferSize = value}
            />
          </Panel>
        </Col>

        <Col md={5}>
          <ValueSelector
            label="Benchmark Id" colSpans={[4,8]} placeholder="Optional, auto generated if missing"
            onChange={value => this.state.benchmarkRequestId = value}
          />

          <Panel header="Clients">
            <DropdownSelector
              label="Geo Location" options={config.ec2Regions}
              onChange={value => this.setState({clientsGeoLocation: value})}
            />

            <DropdownSelector
              label="Host Type" options={config.ec2InstanceTypes}
              onChange={value => this.setState({clientsHostType: value})}
            />

            <RangeSelector
              label="Clients (count)" placeholders={["Per Host", "Total"]}
              onValidate={value => validator.validateIntRangeWithinRange(value, {min: 1},
                    {validateSubmittable: this.state.failedValidation, omitSuccessEffect: true, allowSuccessorEqualOrLesser: true})}
              onChange={value => this.setState({clientsPerHost: value.min, clientCount: value.max})}
            />

            <RangeSelector
              label="Payload (bytes)"
              onValidate={value => this.validateRange(value, {min: 1})}
              onChange={value => this.setState({payloadSize: value})}
            />

            <RangeSelector
              label="Delay Between Sends (millis)"
              onValidate={value => this.validateRange(value, {min: 1})}
              onChange={value => this.setState({delayMillisBetweenSends: value})}
            />

            <RangeSelector
              label="Delay Creating Client (millis)"
              onValidate={value => this.validateRange(value, {min: 0})}
              onChange={value => this.setState({delayMillisBeforeCreatingClient: value})}
            />

            <RangeSelector
              label="Client Lifespan (millis)"
              onValidate={value => this.validateRange(value, {min: 1})}
              onChange={value => this.setState({clientLifeMillis: value})}
            />
          </Panel>

          <Button bsStyle="primary" className="pull-right" disabled={this.state.pendingSubmission}
                  onClick={() => this.handleStartBenchmarkClicked()}>
            Start Benchmark
          </Button>
        </Col>
      </Grid>
    );
  }
}
