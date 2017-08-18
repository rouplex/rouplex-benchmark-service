import React from 'react';
import Form from 'react-bootstrap/lib/Form';
import FormGroup from 'react-bootstrap/lib/FormGroup';
import FormControl from 'react-bootstrap/lib/FormControl';
import Col from 'react-bootstrap/lib/Col';
import ControlLabel from 'react-bootstrap/lib/ControlLabel';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import Tooltip from 'react-bootstrap/lib/Tooltip';

export default class ValueSelector extends React.Component {
  constructor() {
    super();
    this.value = null;
  }

  getColSpan(i) {
    return this.props.colSpans ? this.props.colSpans[i] : 6;
  }

  render() {
    return (
      <Form horizontal>
        <FormGroup validationState={this.props.onValidate ? this.props.onValidate(this.value) : null}>
          <Col componentClass={ControlLabel} md={this.getColSpan(0)}>
            {this.props.label}
          </Col>
          <Col md={this.getColSpan(1)}>
            <OverlayTrigger placement="right" delayShow={2000} overlay={<Tooltip id="0">{this.props.placeholder}</Tooltip>}>
              <FormControl
                type="text" placeholder={this.props.placeholder} disabled={this.props.disabled}
                onChange={value => {this.value = value.target.value; this.setState({}); this.props.onChange(this.value)}}
              />
            </OverlayTrigger>
          </Col>
        </FormGroup>
      </Form>
    );
  }
}
