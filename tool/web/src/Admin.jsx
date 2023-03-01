import './Admin.css';
import React from 'react';  

import Config from './config.js';

class Admin extends React.Component {

  constructor() {
    super();
    this.state = { samples: [], studies: [], users: [] };

    this.emailRef = React.createRef();
    this.isAdminRef = React.createRef();

    this.studyNameRef = React.createRef();
    this.studyTypeRef = React.createRef();
  }

  componentDidMount() {
    this.fetchSamples();
    this.fetchStudies();
    this.fetchUsers();
  }  

  fetchSamples() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/sample/getall', requestOptions)
      .then(response => {return response.json()})
      .then(
        data => {
          this.setState(prevState => ({ samples: data, studies: prevState.studies, users: prevState.users }))
          this.samples = data;
        }
      );
  }

  fetchStudies() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/study/getall', requestOptions)
      .then(response => {return response.json()})
      .then(
        data => {
          this.setState(prevState => ({ samples: prevState.samples, studies: data, users: prevState.users }))
        }
      );
  }

  synchronizeSamples() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/sample/synchronizesamples', requestOptions)
      .then(response => {this.fetchSamples(); this.fetchUsers();});
  }

  fetchUsers() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/authentication/getallusers', requestOptions)
      .then(response => {return response.json()})
      .then(
        data => {
          this.setState(prevState => ({ samples: prevState.samples, users: data }))
        }
      );    
  }

  createUser() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString, email: this.emailRef.current.value, isAdmin: this.isAdminRef.current.checked })
    };

    fetch(Config.apiBaseUrl + '/authentication/createuser', requestOptions)
      .then(response => {this.fetchUsers()});

    this.emailRef.current.value = "";
    this.isAdminRef.current.checked = false;
  }  

  createStudy() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString, name: this.studyNameRef.current.value, type: this.studyTypeRef.current.value })
    };

    fetch(Config.apiBaseUrl + '/study/create', requestOptions)
      .then(response => {this.fetchStudies()});

    this.emailRef.current.value = "";
    this.isAdminRef.current.checked = false;
  }  


  userHasStudy(user, study) {
    for (var i = 0; i < user.studies.length; i++) {
      if (user.studies[i].id == study.id) {
        return true;
      }
    }

    return false;
  }

  studyHasSample(study, sample) {
    for (var i = 0; i < study.samples.length; i++) {
      if (study.samples[i].name == sample.name) {
        return true;
      }
    }

    return false;
  }  

  clickUserStudy(user, study) {
    if (this.userHasStudy(user, study)) {
      const requestOptions = {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ tokenString: this.props.token.tokenString, userId: user.id, studyId: study.id })
      };

      fetch(Config.apiBaseUrl + '/study/removefromuser', requestOptions)
        .then(response => {this.fetchUsers()});
    } else {
      const requestOptions = {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ tokenString: this.props.token.tokenString, userId: user.id, studyId: study.id })
      };

      fetch(Config.apiBaseUrl + '/study/addtouser', requestOptions)
        .then(response => {this.fetchUsers()});      
    }
  } 

  clickStudySample(study, sample) {
    if (this.studyHasSample(study, sample)) {
      const requestOptions = {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ tokenString: this.props.token.tokenString, sampleId: sample.id, studyId: study.id })
      };

      fetch(Config.apiBaseUrl + '/sample/removefromstudy', requestOptions)
        .then(response => {this.fetchStudies()});
    } else {
      const requestOptions = {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ tokenString: this.props.token.tokenString, sampleId: sample.id, studyId: study.id })
      };

      fetch(Config.apiBaseUrl + '/sample/addtostudy', requestOptions)
        .then(response => {this.fetchStudies()});      
    }
  }   

  deleteStudy(study) {
      const requestOptions = {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ tokenString: this.props.token.tokenString, studyId: study.id })
      };

      fetch(Config.apiBaseUrl + '/study/delete', requestOptions)
        .then(response => {this.fetchStudies()});    
  }

  deleteUser(user) {
      const requestOptions = {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ tokenString: this.props.token.tokenString, userId: user.id })
      };

      fetch(Config.apiBaseUrl + '/authentication/deleteuser', requestOptions)
        .then(response => {this.fetchUsers()});    
  }

  getSampleByName(sample) {
    for (var i = 0; i < this.samples.length; i++) {
      if (this.samples[i].name == sample) {
        return this.samples[i];
      }
    }
  }

  changeSampleType(name, e) {
    let value = e.target.value;
    console.log(this.getSampleByName(name));
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString, name: name, type: value, igvPath: this.getSampleByName(name).igvPath })
    };

    this.getSampleByName(name).type = value;

    fetch(Config.apiBaseUrl + '/sample/update', requestOptions)
      .then(response => {this.fetchUsers()});       
  }

  deleteSample(name, e) {
    let value = e.target.value;
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    this.getSampleByName(name).type = value;

    fetch(Config.apiBaseUrl + '/sample/delete/' + name, requestOptions)
      .then(response => {this.fetchSamples()});
  }

  changeIgvPath(name, e) {    
    let value = e.target.value;
    console.log(this.getSampleByName(name));
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString, name: name, type: this.getSampleByName(name).type, igvPath: value })
    };

    this.getSampleByName(name).igvPath = value;

    fetch(Config.apiBaseUrl + '/sample/update', requestOptions)
      .then(response => {this.fetchUsers()});       
  }  

  render() {
    return <div className="Admin">
        <h1>Admin</h1>
        <hr/>
        <h2>Samples</h2>
        <button onClick={(e) => this.synchronizeSamples()} className="small"><i className="glyphicon bi bi-arrow-clockwise"></i> Synchronize Datasets</button> This may take some time, click the button only once<br/>
        <table> 
          {
            this.state.samples.map((item, index) => {
              return <tr>
                  <td><i className="glyphicon bi bi-bar-chart-steps"></i> {item.name}</td>
                  <td>
                    <select onChange={(e) => this.changeSampleType(item.name, e)}>
                      <option value="">Normal</option>
                      <option selected={item.type == "trio"} value="trio">Trio</option>
                    </select>
                  </td>
                  <td>
                    <input className={"igvPath"} defaultValue={item.igvPath} onChange={(e) => this.changeIgvPath(item.name, e)} placeholder=".cram or .bam path"/>
                  </td>
                  <td>
                    <button className="small" onClick={(e) => {if (window.confirm('Are you sure you want to delete ' + item.name + '?')) this.deleteSample(item.name, e)} }>Delete</button>
                  </td>
                </tr>
            })
          }
        </table>
        <hr/>
        <h2>Studies</h2>
        <b>Add Study</b> 
        <input ref={this.studyNameRef} type="text" placeholder="Name" /> 
        <select ref={this.studyTypeRef}>
          <option value="">Normal</option>
          <option value="cancer">Cancer</option>
        </select>
        <button onClick={(e) => this.createStudy()} className="small">Create Study</button><br/>
        <h3>All studies</h3>
        <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Samples</th>
          </tr>
        </thead>
        {
          this.state.studies.map((study, index) => {
            return <tr>
                <td>{study.name}</td>
                <td>{study.type}</td>
                <td>
                  <div className="sampleBox">
                    {
                      this.state.samples.map((sample, index) => {
                        return <div><input onChange={(e) => this.clickStudySample(study, sample)} type="checkbox" checked={this.studyHasSample(study, sample)} /> {sample.name}</div>
                      })
                    }
                  </div>
                </td>
                <td><button className="small" onClick={(e) => this.deleteStudy(study)}>Delete</button></td>
              </tr>
          })
        }
        </table>
        <hr/>        
        <h2>Users</h2>
        <b>Add User</b> 
        <input ref={this.emailRef} type="email" placeholder="email" /> 
        <input ref={this.isAdminRef} type="checkbox" /> <label>Admin</label>
        <button onClick={(e) => this.createUser()} className="small">Create User</button><br/>
        <h3>All users</h3>
        <table>
        <thead>
          <tr>
            <th>Email</th>
            <th>Is Admin</th>
            <th>Is Active</th>
            <th>Activation Link</th>
            <th>Studies</th>
            <th>Action</th>
          </tr>
        </thead>
        {
          this.state.users.map((user, index) => {
            let activation = "";
            if (!user.isActive) {
              activation = Config.appBaseUrl + "/activate/" + user.activationCode;
            }
            return <tr>
                <td>{user.email}</td>
                <td>{user.isAdmin ? "true" : ""}</td>
                <td>{user.isActive  ? "true" : ""}</td>
                <td>{activation}</td>
                <td>
                  <div className="sampleBox">
                    {
                      this.state.studies.map((study, index) => {
                        return <div><input onChange={(e) => this.clickUserStudy(user, study)} type="checkbox" checked={this.userHasStudy(user, study)} /> {study.name}</div>
                      })
                    }
                  </div>
                </td>
                <td><button className="small" onClick={(e) => this.deleteUser(user)}>Delete</button></td>
              </tr>
          })
        }
        </table>
        <hr/>
      </div>;
  } 

}

export default Admin;
