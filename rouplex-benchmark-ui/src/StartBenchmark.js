import React from 'react';
import Panel from 'react-bootstrap/lib/Panel';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import Button from 'react-bootstrap/lib/Button';
import Checkbox from 'react-bootstrap/lib/Checkbox';

import NioProviderSelector from './components/NioProviderSelector';
import DropdownSelector from './components/DropdownSelector';
import RangeSelector from './components/RangeSelector';
import ValueSelector from './components/ValueSelector';
import LabeledValue from './components/LabeledValue';
import BenchmarkGraphs from './BenchmarkGraphs';

var config = require("./Config.js");
var validator = require("./components/Validator.js");

export default class StartBenchmark extends React.Component {
  // this.props.sessionInfo
  // this.props.onPathUpdate
  constructor() {
    super();

    this.state = {
      connectParams: null,
      transferParams: null,
      serverDeployed: false,
      pendingSubmission: false,
      failedSubmission: false,
      failedValidation: false
    };

    this.sourceParams = {
      ssl: false
    }
  }

  getConnectParams() {
    var clientCount = this.parseIntValue("Clients", this.sourceParams.clientCount);
    var clientsPerHost = this.parseIntValue("Clients", this.sourceParams.clientsPerHost);
    var minDelayMillisBeforeCreatingClient = this.parseIntValue("Delay Creating Client", this.sourceParams.delayMillisBeforeCreatingClient.min);
    var maxDelayMillisBeforeCreatingClient = this.parseIntValue("Delay Creating Client", this.sourceParams.delayMillisBeforeCreatingClient.max);
    var minClientLifeMillis = this.parseIntValue("Client Lifespan", this.sourceParams.clientLifeMillis.min);
    var maxClientLifeMillis = this.parseIntValue("Client Lifespan", this.sourceParams.clientLifeMillis.max);

    if (clientCount <= 0) {
      throw "Total Clients must be positive"
    }

    if (clientsPerHost <= 0) {
      throw "Clients Per Host must be positive"
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

    return {
      clientCount: clientCount,
      clientsPerHost: clientsPerHost,
      minDelayMillisBeforeCreatingClient: minDelayMillisBeforeCreatingClient,
      maxDelayMillisBeforeCreatingClient: maxDelayMillisBeforeCreatingClient,
      minClientLifeMillis: minClientLifeMillis,
      maxClientLifeMillis: maxClientLifeMillis
    }
  }

  getTransferParams() {
    var minPayloadSize = this.parseIntValue("Payload", this.sourceParams.payloadSize.min);
    var maxPayloadSize = this.parseIntValue("Payload", this.sourceParams.payloadSize.max);
    var minDelayMillisBetweenSends = this.parseIntValue("Delay Between Sends", this.sourceParams.delayMillisBetweenSends.min);
    var maxDelayMillisBetweenSends = this.parseIntValue("Delay Between Sends", this.sourceParams.delayMillisBetweenSends.max);

    if (minPayloadSize <= 0) {
      throw "Minimum Payload Size must be positive"
    }
    if (maxPayloadSize <= minPayloadSize) {
      throw "Maximum Payload Size must be greater than Minimum Payload Size"
    }

    if (minDelayMillisBetweenSends < 0) {
      throw "Minimum Delay Between Sends must be non-negative"
    }
    if (maxDelayMillisBetweenSends <= minDelayMillisBetweenSends) {
      throw "Maximum Delay Between Sends must be greater than Minimum Delay Between Sends"
    }

    return {
      minPayloadSize: minPayloadSize,
      maxPayloadSize: maxPayloadSize,
      minDelayMillisBetweenSends: minDelayMillisBetweenSends,
      maxDelayMillisBetweenSends: maxDelayMillisBetweenSends,
      echoRatio: this.sourceParams.echoRatio
    }
  }

  updateState() {
    try {
      this.setState({connectParams: this.getConnectParams()});
    } catch (e) {
      this.setState({
        connectParams: null,
        transferParams: null
      });
      return
    }

    try {
      this.setState({transferParams: this.getTransferParams()});
    } catch (e) {
      this.setState({transferParams: null});
    }
  }

  buildRequestBody() {
    var connectParams = this.getConnectParams();
    var transferParams = this.getTransferParams();

    var keyNameQuoted = this.quote(this.sourceParams.keyName);
    var imageIdQuoted = this.quote(this.sourceParams.imageId);
    var socketSendBufferSize = this.parseIntValue("Socket Send Buffer Size", this.sourceParams.socketSendBufferSize, 0);
    var socketReceiveBufferSize = this.parseIntValue("Socket Receive Buffer Size", this.sourceParams.socketReceiveBufferSize, 0);
    var backlog = this.parseIntValue("Backlog", this.sourceParams.backlog, 0);
    var echoRatioQuoted = this.quote(transferParams.echoRatio);
    var tcpMemoryAsPercentOfTotal = this.parseIntValue("Memory As Percent Of Total", this.sourceParams.tcpMemoryAsPercentOfTotal, 0);

    if (socketSendBufferSize < 0) {
      throw "Socket Send Buffer Size must be non-negative"
    }

    if (socketReceiveBufferSize < 0) {
      throw "Socket Receive Buffer Size must be non-negative"
    }

    if (backlog < 0) {
      throw "Backlog must be non-negative"
    }

    if (this.state.serverDeployed) {
      var serverIpAddress = this.sourceParams.serverIpAddress;
      var serverPort = this.sourceParams.serverPort;
    } else {
      var serverIpAddress = null;
      var serverPort = 8888;
    }

    var serverIpAddressQuoted = this.quote(serverIpAddress);
    var samePlacementGroup = this.sourceParams.ec2PlacementGroup === '' ? null : this.sourceParams.ec2PlacementGroup;

    var requestBody =
      '{\n' +
      '  "echoRatio" : ' + echoRatioQuoted + ',\n' +
      '  "serverHostType" : "' + this.sourceParams.serverHostType + '",\n' +
      '  "serverGeoLocation" : "' + this.sourceParams.serverGeoLocation + '",\n' +
      '  "clientsHostType" : "' + this.sourceParams.clientsHostType + '",\n' +
      '  "clientsGeoLocation" : "' + this.sourceParams.clientsGeoLocation + '",\n' +
      '  "imageId" : ' + imageIdQuoted + ',\n' +
      '  "samePlacementGroup" : ' + samePlacementGroup + ',\n' +
      '  "keyName" : ' + keyNameQuoted + ',\n' +
      '  "tcpMemoryAsPercentOfTotal" : ' + tcpMemoryAsPercentOfTotal + ',\n' +
      '  "provider" : "' + this.sourceParams.provider + '",\n' +
      '  "serverIpAddress" : ' + serverIpAddressQuoted + ',\n' +
      '  "port" : ' + serverPort + ',\n' +
      '  "ssl" : "' + this.sourceParams.ssl + '",\n' +
      '  "socketSendBufferSize" : ' + socketSendBufferSize + ',\n' +
      '  "socketReceiveBufferSize" : ' + socketReceiveBufferSize + ',\n' +
      '  "backlog" : ' + backlog + ',\n' +
      '  "clientCount" : ' + connectParams.clientCount + ',\n' +
      '  "clientsPerHost" : ' + connectParams.clientsPerHost + ',\n' +
      '  "minPayloadSize" : ' + transferParams.minPayloadSize + ',\n' +
      '  "maxPayloadSize" : ' + transferParams.maxPayloadSize + ',\n' +
      '  "minDelayMillisBetweenSends" : ' + transferParams.minDelayMillisBetweenSends + ',\n' +
      '  "maxDelayMillisBetweenSends" : ' + transferParams.maxDelayMillisBetweenSends + ',\n' +
      '  "minDelayMillisBeforeCreatingClient" : ' + connectParams.minDelayMillisBeforeCreatingClient + ',\n' +
      '  "maxDelayMillisBeforeCreatingClient" : ' + connectParams.maxDelayMillisBeforeCreatingClient + ',\n' +
      '  "minClientLifeMillis" : ' + connectParams.minClientLifeMillis + ',\n' +
      '  "maxClientLifeMillis" : ' + connectParams.maxClientLifeMillis + '\n' +
      '}\n';

    return requestBody;
  }

  postRequest(requestBody) {
    var postRequest = new XMLHttpRequest();
    postRequest.addEventListener("load", () => {
      console.log("startTcpBenchmark.post.response: " + postRequest.status + " " + postRequest.responseText);
      //alert("startTcpBenchmark.post.response: " + postRequest.responseText);
      this.setState({pendingSubmission: false}); // failedSubmission is set in following statements

      if (postRequest.status != 200) {
        this.setState({failedSubmission: true});
        return alert("Could not start benchmark. Details: " + postRequest.responseText);
      }

      try {
        var response = JSON.parse(postRequest.responseText);
      } catch (err) {
        this.setState({failedSubmission: true});
        return alert("Non parsable server response. Details: " + err);
      }

      this.setState({failedSubmission: false});
      this.props.onPathUpdate("/benchmark/show#" + response.id)
    });

    postRequest.addEventListener("error", () => {
      console.log("startTcpBenchmark.post.error: " + postRequest.responseText);
      //alert("startTcpBenchmark.post.error: " + postRequest.responseText);
      this.setState({
        pendingSubmission: false,
        failedSubmission: true
      });

      alert("Could not start benchmark. Cause: " + postRequest.responseText);
    });

    if (this.sourceParams.benchmarkId == null) {
      postRequest.open("POST", config.tcpEchoBenchmarkUrl);
    } else {
      postRequest.open("PUT", config.tcpEchoBenchmarkUrl + "/" + encodeURIComponent(this.sourceParams.benchmarkId));
    }

    postRequest.setRequestHeader('Content-Type', 'application/json');
    postRequest.setRequestHeader('Accept', 'application/json');
    postRequest.setRequestHeader('Rouplex-Cookie-Enabled', navigator.cookieEnabled);
    postRequest.setRequestHeader('Rouplex-SessionId', this.props.sessionInfo.sessionId);

    console.log("startTcpBenchmark.put-or-post.request: " + requestBody);
    postRequest.send(requestBody);
  }

  handleStartBenchmarkClicked() {
    this.setState({
      pendingSubmission: true,
      failedSubmission: false
    });

    try {
      var requestBody = this.buildRequestBody();
      this.setState({failedValidation: false});
      this.postRequest(requestBody);
    }
    catch (err) {
      alert("Error starting benchmark. Cause: " + err);
      this.setState({
        pendingSubmission: false,
        failedSubmission: true
      });
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

  checkNoSlash(stringValue) {
    if (!validator.isUndefinedNullOrEmpty(stringValue) && stringValue.indexOf("/") != -1) {
      return "error";
    }
  }

  validateRange(value, range) {
    return validator.validateIntRangeWithinRange(value, range,
      {validateSubmittable: this.state.failedValidation, omitSuccessEffect: true});
  }

  calculateComputeCostForInstances(instanceType, count) {
    var costPerInstance = 0;
    config.ec2InstanceTypes.map(function (instance) {
      if (instance.key === instanceType) {
        costPerInstance = instance.cost;
      }
    });

    return costPerInstance * count;
  }

  calculateComputeCost() {
    var cp = this.state.connectParams;
    if (!cp) {
      return 0;
    }

    var costPerHour = 0;
    var clientHosts = cp.clientCount / cp.clientsPerHost;

    if (validator.validateIntValueWithinRange(clientHosts, {min: 0}, {validateSubmittable: true}) === "success") {
      costPerHour += this.calculateComputeCostForInstances(this.sourceParams.clientsHostType, clientHosts);
    }

    if (!this.sourceParams.serverDeployed) {
      costPerHour += this.calculateComputeCostForInstances(this.sourceParams.serverHostType, 1);
    }

    var benchmarkMillis = cp.maxDelayMillisBeforeCreatingClient + cp.maxClientLifeMillis;
    benchmarkMillis += 1000 * 10 * 60; // 10 mins for setup
    return (Math.ceil(benchmarkMillis / 3600000) * costPerHour).toFixed(2);
  }

  getBenchmarkDuration() {
    var benchmarkMillis = this.state.connectParams
      ? this.state.connectParams.maxDelayMillisBeforeCreatingClient
        + this.state.connectParams.maxClientLifeMillis + 1000 * 10 * 60 : 0;

    return this.convertMillisUp(benchmarkMillis);
  }

  convertMillisUp(timeValue) {
    var unitNames = [" Millis", " Secs", " Mins", " Hrs", " Days"];
    var unitRatios = [1000, 60, 60, 24];

    var index = 0;
    while (index < unitNames.length && timeValue >= 10 * unitRatios[index]) {
      timeValue = Math.floor(timeValue / unitRatios[index]);
      index++;
    }

    return timeValue + unitNames[index];
  }

  render() {
    return (
      <Grid style={{padding: '0'}}>
        <Row>
          <Col md={5}>
            <NioProviderSelector
              nioProviders={config.nioProviders}
              onSslChange={value => this.sourceParams.ssl = value}
              onNioProviderChange={value => this.sourceParams.provider = value}
            />

            <Panel header="Server">
              <DropdownSelector
                label="Geo Location" colSpans={[5,7]} options={config.ec2Regions}
                disabled={this.state.serverDeployed}
                onChange={value => this.sourceParams.serverGeoLocation = value}
              />

              <DropdownSelector
                label="Host Type" colSpans={[5,7]} options={config.ec2InstanceTypes}
                disabled={this.state.serverDeployed}
                onChange={value => this.sourceParams.serverHostType = value}
              />

              <ValueSelector
                label="Backlog (int)" colSpans={[5,7]} placeholder="Optional, defaults to system's"
                disabled={this.state.serverDeployed}
                onChange={value => {
                  this.sourceParams.backlog = value;
                  return this.validateValue(value, {min: 0, optional: true});
                }}
              />

              <ValueSelector
                label="Echo Ratio (x:y)" colSpans={[5,7]} placeholder="Optional, defaults to 1:1 (n/a yet)"
                disabled={this.state.serverDeployed}
                onChange={value => {
                  this.sourceParams.echoRatio = value;
                  this.updateState();
                }}
              />

              <Row>
                <Col mdOffset={5} md={7}>
                  <Checkbox style={{marginTop: '0'}}
                            onChange={key => this.setState({serverDeployed: key.target.checked})}>
                    &nbsp; Server already running
                  </Checkbox>
                </Col>
              </Row>

              <RangeSelector
                label="Server Address" disabled={!this.state.serverDeployed}
                colSpans={[5,4,3]} placeholders={["IP (xx.xx.xx.xx)", "Port"]}
                onChange={value => {
                  this.sourceParams.serverIpAddress = value.min;
                  this.sourceParams.serverPort = value.max;
                  this.updateState();
                }}
              />
            </Panel>

            <Panel header="Extra Parameters">
              <ValueSelector
                label="Aws Key Name" colSpans={[7,5]} placeholder="Optional, for ssh host access"
                onChange={value => this.sourceParams.keyName = value}
              />

              <ValueSelector
                label="Image Id" colSpans={[7,5]} placeholder="Optional, defaults to latest"
                onChange={value => this.sourceParams.imageId = value}
              />

              <ValueSelector
                label="Tcp Memory (% of total)" colSpans={[7, 5]} placeholder="Optional, defaults to system's"
                onChange={value => {
                  this.sourceParams.tcpMemoryAsPercentOfTotal = value;
                  return this.validateValue(value, {min: 0, optional: true});
                }}
              />

              <ValueSelector
                label="Socket Send Buffer Size (bytes)" colSpans={[7, 5]} placeholder="Optional, defaults to system's"
                onChange={value => {
                  this.sourceParams.socketSendBufferSize = value;
                  return this.validateValue(value, {min: 0, optional: true});
                }}
              />

              <ValueSelector
                label="Socket Receive Buffer Size (bytes)" colSpans={[7,5]} placeholder="Optional, defaults to system's"
                onChange={value => {
                  this.sourceParams.socketReceiveBufferSize = value;
                  return this.validateValue(value, {min: 0, optional: true});
                }}
              />
            </Panel>
          </Col>

          <Col md={5}>
            <ValueSelector
              label="Benchmark Id" colSpans={[4,8]} placeholder="Optional, auto generated if missing"
              onChange={value => {
                this.sourceParams.benchmarkId = value;
                return this.checkNoSlash(value);
              }}
            />

            <Panel header="Clients">
              <DropdownSelector
                label="Geo Location" options={config.ec2Regions}
                onChange={value => this.sourceParams.clientsGeoLocation = value}
              />

              <DropdownSelector
                label="Host Type" options={config.ec2InstanceTypes}
                onChange={value => this.sourceParams.clientsHostType = value}
              />

              <DropdownSelector
                label="Placement" options={config.ec2PlacementGroups}
                onChange={value => this.sourceParams.ec2PlacementGroup = value}
              />

              <RangeSelector
                label="Clients (count)" placeholders={["Per Host", "Total"]}
                onChange={value => {
                  this.sourceParams.clientsPerHost = value.min;
                  this.sourceParams.clientCount = value.max;
                  this.updateState();

                  return validator.validateIntRangeWithinRange(value, {min: 1}, {
                    validateSubmittable: this.state.failedValidation,
                    omitSuccessEffect: true,
                    allowSuccessorEqualOrLesser: true
                  });
                }}
              />

              <RangeSelector
                label="Client Arrivals (millis)"
                onChange={value => {
                  this.sourceParams.delayMillisBeforeCreatingClient = value;
                  this.updateState();
                  return this.validateRange(value, {min: 0});
                }}
              />

              <RangeSelector
                label="Client Lifespan (millis)"
                onChange={value => {
                  this.sourceParams.clientLifeMillis = value;
                  this.updateState();
                  return this.validateRange(value, {min: 0});
                }}
              />

              <RangeSelector
                label="Payload (bytes)"
                onChange={value => {
                  this.sourceParams.payloadSize = value;
                  this.updateState();
                  return this.validateRange(value, {min: 0});
                }}
              />

              <RangeSelector
                label="Delay Between Sends (millis)"
                onChange={value => {
                  this.sourceParams.delayMillisBetweenSends = value;
                  this.updateState();
                  return this.validateRange(value, {min: 0});
                }}
              />
            </Panel>

            <Panel header={"Approximate cost (" + this.getBenchmarkDuration() + ")"}>
              <LabeledValue label="Compute" value={"$" + this.calculateComputeCost()} colSpans={[6,6]}/>
              <LabeledValue label="Network" value="$0 (n/a yet)" colSpans={[6,6]}/>
              <LabeledValue label="Other" value="$0 (n/a yet)" colSpans={[6,6]}/>
            </Panel>

            <Button bsStyle="primary" className="pull-right" disabled={this.state.pendingSubmission}
                    bsStyle={this.state.failedSubmission ? "warning" : "primary"}
                    onClick={() => this.handleStartBenchmarkClicked()}>
              Start Benchmark
            </Button>
          </Col>
        </Row>
        <Row>
          <Col md={10}>
            <BenchmarkGraphs
              connectParams={this.state.connectParams}
              transferParams={this.state.transferParams}
            />
          </Col>
        </Row>
      </Grid>
    );
  }
}
