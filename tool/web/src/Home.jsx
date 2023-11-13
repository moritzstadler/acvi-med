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
          <div className="info"><i class="bi bi-info-circle-fill"></i> These are your studies. Click on a study to view its patient samples. If you are not seeing your studies, talk to your administrator.</div>
          <hr/>
          {
            this.state.studies.map((item, i) => {
              return <div className="studyBox">
                  <div onClick={(e) => this.toggleVisible(i)}><i className={"glyphicon bi bi-caret-" + (this.state.visible[i] ? "down" : "right") + "-fill"} ></i> {item.name} {item.type}</div>
                  <div className={"studies " + (this.state.visible[i] ? "" : "hidden")} >
                    {
                      item.samples.map((sample, j) => {
                        return <div className="sampleHomeBox"><div style={{display: "inline-block", width: "25%"}}><b>{sample.name}</b></div><div style={{display: "inline-block", width: "75%", "text-align": "right"}}><a className="buttonLink" href={"/sample/" + sample.name}>Variant-Explorer</a> <a className="buttonLink" href={"/tiering/" + sample.name}>Quick-Tiering</a> <a className="buttonLink" href={"/secondaryfindings/" + sample.name}>Secondary Findings</a> <a className="buttonLink" href={"/pharmacogenomics/" + sample.name}>Pharmacogenomics</a></div></div>
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
