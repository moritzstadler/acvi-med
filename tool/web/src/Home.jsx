import './Home.css';
import React from 'react';  

import Config from './config.js';

class Home extends React.Component {

  constructor() {
    super();
    this.state = { studies: [], visible: [], message: "Loading..." };
  }

  componentDidMount() {
    this.fetchSamples();
  }  

  fetchSamples() {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/study/getforuser', requestOptions)
      .then(
        response => {
          return response.json()
        }
      )
      .then(
        data => {
          if (data.length <= 0) {
            this.setState(prevState => ({studies: [], visible: [], message: "You have no studies"}))
          } else {
            let visible = [];
            for (var i = 0; i < data.length; i++) {
              visible.push(false);
            }
            this.setState(prevState => ({ studies: data, visible: visible, message: "" }));
          }
        }
      );

  }

  toggleVisible(index) {
    let visible = this.state.visible;
    visible[index] = !visible[index];
    this.setState(prevState => ({ studies: prevState.studies, visible: visible, message: "" }));
  }

  render() {
    return <div className="Home">
      <div className="samplesBackground">
        <div className="samplesBox">
          <h1>Your Studies</h1>
          <hr/>
          {
            this.state.studies.map((item, i) => {
              return <div className="studyBox">
                  <div onClick={(e) => this.toggleVisible(i)}><i className={"glyphicon bi bi-caret-" + (this.state.visible[i] ? "down" : "right") + "-fill"} ></i> {item.name} {item.type}</div>
                  <div className={"studies " + (this.state.visible[i] ? "" : "hidden")} >
                    {
                      item.samples.map((sample, j) => {
                        return <div><a href={"/sample/" + sample.name}>{sample.name}</a></div>
                      })
                    }
                  </div>
                </div>
            })
          }
          {this.state.message}
        </div>
      </div>
    </div>;
  } 

}

export default Home;
