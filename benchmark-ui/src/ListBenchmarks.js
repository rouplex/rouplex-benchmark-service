import React from 'react';
import Table from 'react-bootstrap/lib/Panel';
import Grid from 'react-bootstrap/lib/Grid';
import Row from 'react-bootstrap/lib/Row';
import Col from 'react-bootstrap/lib/Col';
import Button from 'react-bootstrap/lib/Button';

import NioProviderSelector from './components/NioProviderSelector';
import DropdownSelector from './components/DropdownSelector';
import RangeSelector from './components/RangeSelector';
import ValueSelector from './components/ValueSelector';

var config = require("./Config.js");

export default class ListBenchmarks extends React.Component {

  render() {
    return (
      <Grid>
        <Col md={10}>
          <Table striped bordered condensed hover>
            <thead>
            <tr>
              <th>#</th>
              <th>First Name</th>
              <th>Last Name</th>
              <th>Username</th>
            </tr>
            </thead>
            <tbody>
            <tr>
              <td>1</td>
              <td>Mark</td>
              <td>Otto</td>
              <td>@mdo</td>
            </tr>
            <tr>
              <td>2</td>
              <td>Jacob</td>
              <td>Thornton</td>
              <td>@fat</td>
            </tr>
            <tr>
              <td>3</td>
              <td colSpan="2">Larry the Bird</td>
              <td>@twitter</td>
            </tr>
            </tbody>
          </Table>
        </Col>
      </Grid>
    );
  }
}
