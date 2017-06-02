import React from 'react';
import 'bootstrap/less/bootstrap.less';
import './styles/custom-styles.css';

import RouplexHeader from './RouplexHeader';
import RouplexBody from './RouplexBody';
import RouplexFooter from './RouplexFooter';

global.React = React;

React.render(
  <div>
    <RouplexHeader/>
    <RouplexBody />
    <RouplexFooter />
  </div>
  , document.getElementById('app'));
