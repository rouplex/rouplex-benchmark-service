import React from 'react';
import Jumbotron from 'react-bootstrap/lib/Jumbotron';
import Panel from 'react-bootstrap/lib/Panel';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import FormGroup from 'react-bootstrap/lib/FormGroup';
import Form from 'react-bootstrap/lib/Form';
import FormControl from 'react-bootstrap/lib/FormControl';
import Checkbox from 'react-bootstrap/lib/Checkbox';
import ControlLabel from 'react-bootstrap/lib/ControlLabel';
import Button from 'react-bootstrap/lib/Button';
import ButtonToolbar from 'react-bootstrap/lib/ButtonToolbar';
import RouplexDropdownSelector from './RouplexDropdownSelector';
import NioProviderSelector from './NioProviderSelector';
import RangeSelector from './components/RangeSelector';
import ValueSelector from './components/ValueSelector';

var config = require("./Config.js");
var validator = require("./components/Validator.js");

export default class TcpBenchmarkStart extends React.Component {
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
      failedSubmission: false
    }
  }

  handleTcpBenchmarkStartClicked() {
    try {
      var echoRatio = this.sanitize(this.state.echoRatio);
      var benchmarkRequestId = this.sanitize(this.state.benchmarkRequestId);
      var keyName = this.sanitize(this.state.keyName);

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

      var request =
        '{\n' +
        '  "echoRatio" : "' + echoRatio + '",\n' +
        '  "optionalBenchmarkRequestId" : "' + benchmarkRequestId + '",\n' +
        '  "optionalServerHostType" : "' + this.state.serverHostType + '",\n' +
        '  "optionalServerGeoLocation" : "' + this.state.serverGeoLocation + '",\n' +
        '  "optionalClientsHostType" : "' + this.state.clientsHostType + '",\n' +
        '  "optionalClientsGeoLocation" : "' + this.state.clientsGeoLocation + '",\n' +
        '  "optionalImageId" : "",\n' +
        '  "optionalKeyName" : "' + keyName + '",\n' +
        '  "optionalTcpMemoryAsPercentOfTotal" : 0,\n' +
        '  "provider" : "' + this.state.provider + '",\n' +
        '  "port" : 8888,\n' +
        '  "ssl" : "' + this.state.ssl + '",\n' +
        '  "optionalSocketSendBufferSize" : ' + socketSendBufferSize + ',\n' +
        '  "optionalSocketReceiveBufferSize" : ' + socketReceiveBufferSize + ',\n' +
        '  "optionalBacklog" : ' + backlog + ',\n' +
        '  "clientCount" : ' + clientCount + ',\n' +
        '  "clientsPerHost" : ' + clientsPerHost + ',\n' +
        '  "minPayloadSize" : ' + minPayloadSize + ',\n' +
        '  "maxPayloadSize" : ' + maxPayloadSize + ',\n' +
        '  "minDelayMillisBetweenSends" : ' + minDelayMillisBetweenSends + ',\n' +
        '  "maxDelayMillisBetweenSends" : ' + maxDelayMillisBetweenSends + ',\n' +
        '  "minDelayMillisBeforeCreatingClient" : ' + minDelayMillisBeforeCreatingClient + ',\n' +
        '  "maxDelayMillisBeforeCreatingClient" : ' + maxDelayMillisBeforeCreatingClient + ',\n' +
        '  "minClientLifeMillis" : ' + minClientLifeMillis + ',\n' +
        '  "maxClientLifeMillis" : ' + maxClientLifeMillis + ',\n' +
        '}\n';

      alert(request)
      this.setState({failedSubmission: false})
    }
    catch (err) {
      alert("Error creating request. Cause: " + err);
      this.setState({failedSubmission: true})
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

  sanitize(stringValue) {
    return validator.isUndefinedNullOrEmpty(stringValue) ? "" : stringValue;
  }

  validateValue(value, range) {
    return validator.validateIntValueWithinRange(value, range,
      {validateSubmittable: this.state.failedSubmission, omitSuccessEffect: true});
  }

  validateRange(value, range) {
    return validator.validateIntRangeWithinRange(value, range,
      {validateSubmittable: this.state.failedSubmission, omitSuccessEffect: true});
  }

  render() {
    return (
      <Jumbotron>
        <Grid>
          <Row>
            <Col md={2}>
              <p><Button>New Tcp Benchmark »</Button></p>
            </Col>

            <Col md={5}>
              <NioProviderSelector
                onSslChange={value => this.state.ssl = value}
                onNioProviderChange={value => this.state.provider = value}
              />

              <Panel header="Server">
                <RouplexDropdownSelector
                  label="Geo Location" colSpans={[5,7]} options={config.ec2Regions}
                  onChange={value => this.setState({serverGeoLocation: value})}
                />

                <RouplexDropdownSelector
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
                <RouplexDropdownSelector
                  label="Geo Location" options={config.ec2Regions}
                  onChange={value => this.setState({clientsGeoLocation: value})}
                />

                <RouplexDropdownSelector
                  label="Host Type" options={config.ec2InstanceTypes}
                  onChange={value => this.setState({clientsHostType: value})}
                />

                <RangeSelector
                  label="Clients (count)" placeholders={["Per Host", "Total"]}
                  onValidate={value => validator.validateIntRangeWithinRange(value, {min: 1},
                    {validateSubmittable: this.state.failedSubmission, omitSuccessEffect: true, allowSuccessorEqualOrLesser: true})}
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

              <Button bsStyle="primary" className="pull-right" onClick={() => this.handleTcpBenchmarkStartClicked()}>
                Start Tcp Benchmark
              </Button>
            </Col>
          </Row>
        </Grid>
      </Jumbotron>
    )
  }
}
