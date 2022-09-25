import './Authentication.css';
import React from 'react';  
import logo from './logo.svg';

class Authentication extends React.Component {

  constructor() {
    super();
    this.state = { tokenString: null, email: null, expiryTime: null };

    this.emailRef = React.createRef();
    this.passwordRef = React.createRef();
  }

  render() {
    let message = "";

    if (this.props.response != null && !this.props.response.success) {
      message = <div className="errorMessage">The email - password combination is incorrect. If you think this is an error contact your system administrator.</div>;
    }

    return <div className="authenticationBackground">
        <div className="authenticationBox">
          <img className="loginLogo" src={logo} /><br/>
          <input className="large" placeholder="email" ref={this.emailRef} type="text" /><br/>
          <input onKeyPress={(ev) => { if (ev.key === "Enter") { ev.preventDefault(); this.props.login(this.emailRef.current.value, this.passwordRef.current.value); }}} className="large" placeholder="password" ref={this.passwordRef} type="password" /><br/>
          <button className="large" onClick={(e) => this.props.login(this.emailRef.current.value, this.passwordRef.current.value)}><i className="glyphicon bi bi-lock-fill"></i> Login</button><br/>
          {message}
        </div>
      </div>;
  } 

}

export default Authentication;
