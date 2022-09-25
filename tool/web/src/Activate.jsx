import './Activate.css';
import './Authentication.css';
import React from 'react';  
import logo from './logo.svg';

import Config from './config.js';

class Activate extends React.Component {

  constructor() {
    super();
    this.state = { message: "" };

    this.passwordRef = React.createRef();
    this.passwordRepeatRef = React.createRef();
  }  

  checkPasswords() {
    if (this.passwordRef.current.value != this.passwordRepeatRef.current.value) {
      this.setState(prevState => ({ message: "Passwords do not match" }));
    } else if (this.passwordRef.current.value.length <= 0) {
      this.setState(prevState => ({ message: "Enter a password" }));
    } else {
      this.setState(prevState => ({ message: null }));
    }
  }

  fetchActivate() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activationCode: this.props.matchProps.activationCode, password: this.passwordRef.current.value })
    };

    fetch(Config.apiBaseUrl + '/authentication/activateuser', requestOptions)
      .then(
        response => {
          return response.json()
        }
      )
      .then(
        data => {
          window.location.href = Config.appBaseUrl;
        }
      );

  }

  render() {
    return <div className="authenticationBackground">
      <div className="authenticationBox">
        <img className="loginLogo" src={logo} /><br/>
        Welcome! Set your password and remember it carefully.<br/>
        <input onChange={(e) => this.checkPasswords()} className="large" placeholder="password" ref={this.passwordRef} type="password" /><br/>
        <input onChange={(e) => this.checkPasswords()} className="large" placeholder="repeat password" ref={this.passwordRepeatRef} type="password" /><br/>
        <b>{this.state.message}</b><br/>
        <button onClick={(e) => this.fetchActivate()} disabled={this.state.message != null} className={"large " + (this.state.message != null ? "disabled" : "")}><i className="glyphicon bi bi-lock-fill"></i> Set Password</button><br/>
      </div>
    </div>;
  } 

}

export default Activate;
