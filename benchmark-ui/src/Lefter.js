import React from 'react';
import Button from 'react-bootstrap/lib/Button';

var config = require("./Config.js");

export default class Lefter extends React.Component {
  // this.props.sessionInfo
  // this.props.onPathUpdate

  render() {
    return (
      <div>{
        config.paths.map(function (path, key) {
          return (
            <Button key={key} block style={{margin: "8px 0 0 -30px"}} disabled={!this.props.sessionInfo.userInfo}
                    onClick={() => this.props.onPathUpdate(path.path)}>
              {path.label} »
            </Button>
          );
        }, this)}
      </div>
    );
  }
}
