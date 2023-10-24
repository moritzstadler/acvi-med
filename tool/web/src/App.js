import './App.css';

import Activate from './Activate.jsx';
import Admin from './Admin.jsx';
import Note from './Note.jsx';
import Authentication from './Authentication.jsx';
import Home from './Home.jsx';
import StatusBar from './StatusBar.jsx';
import Tiering from './Tiering.jsx';
import SecondaryFindings from './SecondaryFindings.jsx';
import VariantList from './VariantList.jsx';
import View from './View.jsx';

import * as utils from './utils.js'; 
import Config from './config.js';

import React from 'react';

import { BrowserRouter as Router, Route } from 'react-router-dom';

class App extends React.Component {

  componentDidMount() {
    document.title = "Derma Genomics"
    document.description = "ACVI-Med is a tool for analyzing WES and WGS data."
  }

  constructor() {
    super();

    this.state = { loginResponse: undefined, token: undefined };

    let token = localStorage.getItem("session");
    if (token != undefined) {
      this.refresh();
    } else {
      this.state = { loginResponse: null, token: null };      
    }
  }

  login = (email, password) => utils.login(this.tokenRequestCallback, email, password);

  refresh = () => {
    let token = localStorage.getItem("session");
    if (token != undefined) {
      token = JSON.parse(token);
      utils.refresh(this.tokenRequestCallback, token.tokenString);
    }
  }

  tokenRequestCallback = (response) => {
    if (response != null && response.status == null) {
      this.setState(prevState => ({ loginResponse: {success: true}, token: response }));
      localStorage.setItem("session", JSON.stringify(response));
    } else {
      localStorage.clear();
      this.setState(prevState => ({ loginResponse: {success: false}, token: null }));
    }
  }

  logout = () => {
    localStorage.clear();
    this.setState(prevState => ({ loginResponse: null, token: null }));
    window.location.href=Config.appBaseUrl;
  }

  render() {
    //alert(this.state.token);

    if (this.state.token === null) {
      return <Router>
          <Route exact path="/activate" component={() => <Activate />} />
          <Route exact path="/" component={() => <Authentication response={this.state.loginResponse} login={this.login} />} />
          <Route exact path="/view/:name/:pid" component={() => <Authentication response={this.state.loginResponse} login={this.login} />} />
          <Route exact path="/sample/:name" component={() => <Authentication response={this.state.loginResponse} login={this.login} />} />
          <Route exact path="/tiering/:name" component={() => <Authentication response={this.state.loginResponse} login={this.login} />} />
          <Route exact path="/secondaryfindings/:name" component={() => <Authentication response={this.state.loginResponse} login={this.login} />} />
          <Route exact path="/admin" component={() => <Authentication response={this.state.loginResponse} login={this.login} />} />
          <Route exact path="/activate/:activationCode" component={(matchProps) => <Activate matchProps={matchProps.match.params} />} />
        </Router>
    } else if (this.state.token != null) {
      return <Router>
          <Route path="/" component={() => <StatusBar token={this.state.token} logout={this.logout} />} />
          <Route exact path="/" component={() => <Home token={this.state.token} />} />
          <Route exact path="/view/:name/:pid" component={(matchProps) => <View token={this.state.token} matchProps={matchProps.match.params} />} />
          <Route exact path="/sample/:name" component={(matchProps) => <VariantList refresh={this.refresh} token={this.state.token} matchProps={matchProps.match.params} />} />
          <Route exact path="/tiering/:name" component={(matchProps) => <Tiering refresh={this.refresh} token={this.state.token} matchProps={matchProps.match.params} />} />
          <Route exact path="/secondaryfindings/:name" component={(matchProps) => <SecondaryFindings refresh={this.refresh} token={this.state.token} matchProps={matchProps.match.params} />} />
          <Route exact path="/admin" component={() => <Admin token={this.state.token} />} />
          <Route exact path="/note" component={() => <Note token={this.state.token} />} />
        </Router>
    } else {
      return "";
    }
  }
}

export default App;
