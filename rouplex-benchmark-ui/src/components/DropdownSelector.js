import React from 'react';
import Form from 'react-bootstrap/lib/Form';
import FormGroup from 'react-bootstrap/lib/FormGroup';
import FormControl from 'react-bootstrap/lib/FormControl';
import Col from 'react-bootstrap/lib/Col';
import ControlLabel from 'react-bootstrap/lib/ControlLabel';

export default class DropdownSelector extends React.Component {
  constructor() {
    super();
    this.initializing = true;
  }

  getColSpan(i) {
    return this.props.colSpans ? this.props.colSpans[i] : 6;
  }

  render() {
    return (
      <Form horizontal>
        <FormGroup>
          <Col componentClass={ControlLabel} md={this.getColSpan(0)}>
            {this.props.label}
          </Col>
          <Col md={this.getColSpan(1)}>
            <FormControl componentClass="select"
                         onChange={key => this.props.onChange(key.target.value)}
                         inputRef={key => {
                           if (this.initializing && key) {
                             this.initializing = false;
                             this.props.onChange(key.value)
                           }
                         }}>{
                          this.props.options.map(function (option) {
                            return (
                              <option key={option.key} value={option.key}>{option.value}</option>
                            );
                          })}
            </FormControl>
          </Col>
        </FormGroup>
      </Form>
    );
  }
}
