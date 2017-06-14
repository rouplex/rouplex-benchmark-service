import React from 'react';
import Form from 'react-bootstrap/lib/Form';
import FormGroup from 'react-bootstrap/lib/FormGroup';
import FormControl from 'react-bootstrap/lib/FormControl';
import Col from 'react-bootstrap/lib/Col';
import ControlLabel from 'react-bootstrap/lib/ControlLabel';
import Checkbox from 'react-bootstrap/lib/Checkbox';

export default class NioProviderSelector extends React.Component {
  constructor() {
    super();
    this.initializing = true;
  }

  render() {
    return (
      <Form horizontal>
        <FormGroup>
          <Col sm={3}>
            <Checkbox style={{margin: '0 0 0 5px'}} onChange={key => this.props.onSslChange(key.target.checked)}>
              &nbsp; SSL
            </Checkbox>
          </Col>
          <Col componentClass={ControlLabel} sm={2}>
            Provider
          </Col>
          <Col sm={7}>
            <FormControl
              componentClass="select"
              onChange={key => this.props.onNioProviderChange(key.target.value)}
              inputRef={key => {
               if (this.initializing && key) {
                 this.initializing = false;
                 this.props.onNioProviderChange(key.value)
               }
             }}>{
              this.props.nioProviders.map(function (option) {
                return (
                  <option key={option.key} value={option.key}>{option.value}</option>
                )
              })}
            </FormControl>
          </Col>
        </FormGroup>
      </Form>
    );
  }
}
