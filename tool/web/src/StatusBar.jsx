import './StatusBar.css';
import React from 'react';  

import Config from './config.js';

class StatusBar extends React.Component {

  constructor() {
    super();
    this.state = { tokenString: null, email: null, expiryTime: null };
  }

  componentDidMount() {
  }  

  render() {
    let admin = "";

    if (this.props.token.user.isAdmin) {
      admin = <a href={Config.appBaseUrl + "/admin"} >Admin</a>;
    }

    return <div className="statusBar">
      <i className="glyphicon bi bi-lock-fill"></i> Logged in as <b>{this.props.token.user.email}</b> <a href={Config.appBaseUrl} >My Studies</a> {admin} <a href="#" onClick={(e) => this.props.logout()}>Logout</a>
    </div>;
  } 

}

export default StatusBar;
