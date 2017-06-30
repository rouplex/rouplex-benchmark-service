import React from 'react';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';

const style = {textAlign: 'right'};

export default class LabeledValue2 extends React.Component {
  getColSpan(i) {
    if (this.props.colSpans) {
      return this.props.colSpans[i];
    }

    switch (i) {
      case 0: return 5;
      case 1: return 2;
      case 2: return 1;
      case 3: return 2;
      case 4: return 2;
    }
  }

  render() {
    return (
      <Row>
        <Col style={style} md={this.getColSpan(0)}>
          <label>{this.props.label}</label>
        </Col>
        <Col style={style} md={this.getColSpan(1)}>
          {this.props.label1 ? this.props.label1 : "Min"}:
        </Col>
        <Col style={style} md={this.getColSpan(2)}>
          {this.props.value2}
        </Col>
        <Col style={style} md={this.getColSpan(3)}>
          {this.props.label2 ? this.props.label2 : "Max"}:
        </Col>
        <Col style={style} md={this.getColSpan(4)}>
          {this.props.value2}
        </Col>
      </Row>
    );
  }
}
