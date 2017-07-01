import React from 'react';
import Form from 'react-bootstrap/lib/Form';
import FormGroup from 'react-bootstrap/lib/FormGroup';
import FormControl from 'react-bootstrap/lib/FormControl';
import Col from 'react-bootstrap/lib/Col';
import ControlLabel from 'react-bootstrap/lib/ControlLabel';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import Tooltip from 'react-bootstrap/lib/Tooltip';

export default class RangeSelector extends React.Component {
  constructor() {
    super();

    // don't make value part of the state since we must fire the new values right away and setState postpones assigning
    this.value = {}
  }

  getColSpan(i) {
    return this.props.colSpans ? this.props.colSpans[i] : i == 0 ? 6 : 3;
  }

  getPlaceholder(i) {
    return this.props.placeholders ? this.props.placeholders[i] : i == 0 ? "Min (Incl)" : "Max (Excl)";
  }

  render() {
    return (
      <Form horizontal>
        <FormGroup validationState={this.props.onValidate(this.value)}>
          <Col componentClass={ControlLabel} md={this.getColSpan(0)}>
            {this.props.label}
          </Col>
          <Col md={this.getColSpan(1)}>
            <OverlayTrigger placement="right" delayShow={2000} overlay={<Tooltip id="0">{this.getPlaceholder(0)}</Tooltip>}>
              <FormControl
                type="text" placeholder={this.getPlaceholder(0)}
                onChange={min => {this.value.min = min.target.value; this.setState({}); this.props.onChange(this.value)}}
              />
            </OverlayTrigger>
          </Col>
          <Col md={this.getColSpan(2)}>
            <OverlayTrigger placement="right" delayShow={2000} overlay={<Tooltip id="1">{this.getPlaceholder(1)}</Tooltip>}>
              <FormControl
                type="text" placeholder={this.getPlaceholder(1)}
                onChange={max => {this.value.max = max.target.value; this.setState({}); this.props.onChange(this.value)}}
              />
            </OverlayTrigger>
          </Col>
        </FormGroup>
      </Form>
    );
  }
}
