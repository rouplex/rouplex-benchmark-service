import React from 'react';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';

export default class LabeledValue extends React.Component {
  getColSpan(i) {
    return this.props.colSpans ? this.props.colSpans[i] : 6;
  }

  render() {
    return (
      <Row>
        <Col style={{textAlign: 'right'}} md={this.getColSpan(0)}>
          <label>{this.props.label}</label>
        </Col>
        <Col style={{textAlign: 'right'}} md={this.getColSpan(1)}>
          {this.props.value}
        </Col>
      </Row>
    );
  }
}
