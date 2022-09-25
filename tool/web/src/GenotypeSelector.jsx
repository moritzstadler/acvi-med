import './GenotypeSelector.css';
import React from 'react';  

import Config from './config.js';

class GenotypeSelector extends React.Component {

  constructor() {
    super();
  }

  componentDidMount() {
  }

  getFormats(meta) {
    let result = [];

    if (meta == null) {
      return result;
    }

    for (var i = 0; i < meta.length; i++) {
      if (meta[i].id.startsWith("format_")) {
        let startString = "format_";
        let formatName = meta[i].id.substring(meta[i].id.indexOf(startString) + startString.length, meta[i].id.lastIndexOf("_"));
        if (!result.includes(formatName)) {
          result.push(formatName);
        }
      }
    }

    return result;
  } 

  selectChanged(e, item) {
    let name = 'format_' + item + "_gt";
    let value = e.target.value;
    let genotypes = [];

    if (value == 1) {
      genotypes.push({name: name, value: '0/0'});
    } else if (value == 2) {
      genotypes.push({name: name, value: '0/1'});
      genotypes.push({name: name, value: '1/0'});
    } else if (value == 3) {
      genotypes.push({name: name, value: '1/1'});
    } else if (value == 4) {
      genotypes.push({name: name, value: '0/1'});
      genotypes.push({name: name, value: '1/0'});
      genotypes.push({name: name, value: '1/1'});      
    } else if (value == 5) {
      genotypes.push({name: name, value: '0/1'});
      genotypes.push({name: name, value: '1/0'});
      genotypes.push({name: name, value: '0/0'});      
    } else if (value == 6) {
      genotypes.push({name: name, value: '0/0'});
      genotypes.push({name: name, value: '1/1'});
    }  

    this.props.addGenotypesToFilter(genotypes, name);
    this.props.genotypes[item] = value;
  }

  getGenotypes() {
    return this.state.genotypes;
  }

  render() {
    return <div>
        <div className="GenotypeSelector">
          <span className="hint"><i class="bi bi-lightbulb-fill"></i> Select the genotypes or leave at <i>any</i> to select all</span>
          {
            this.getFormats(this.props.meta).map((item) => {
              return <div>
                  <div className="genotype">
                    <span className="patientId">{item}</span>
                    <select className="genotype" onChange={(e) => this.selectChanged(e, item)} >
                      <option value="0" selected={typeof this.props.genotypes == 'undefined' || this.props.genotypes[item] == 0} >any</option>                  
                      <option value="1" selected={typeof this.props.genotypes != 'undefined' && this.props.genotypes[item] == 1} >0/0</option>
                      <option value="2" selected={typeof this.props.genotypes != 'undefined' && this.props.genotypes[item] == 2} >0/1, 1/0</option>
                      <option value="3" selected={typeof this.props.genotypes != 'undefined' && this.props.genotypes[item] == 3} >1/1</option>
                      <option value="4" selected={typeof this.props.genotypes != 'undefined' && this.props.genotypes[item] == 4} >0/1, 1/0, 1/1</option>
                      <option value="5" selected={typeof this.props.genotypes != 'undefined' && this.props.genotypes[item] == 5} >0/1, 1/0, 0/0</option>
                      <option value="6" selected={typeof this.props.genotypes != 'undefined' && this.props.genotypes[item] == 6} >0/0, 1/1</option>
                    </select>
                  </div>
                </div>
            })
          }
        </div>
      </div>;
  }
}

export default GenotypeSelector;