import './View.css';
import React from 'react';  

class ViewField extends React.Component {

  constructor() {
    super();
    this.state = { showInformation: false, expanded: false };
    this.expansionDiv = React.createRef();
  }

  toggleInformation() {
    this.setState(prevState => ({ showInformation: !prevState.showInformation, expanded: prevState.expanded }));
  }

  computePercentWidth(value, item) {
    let percent = 100 * (value - item.from) / (item.to - item.from);
    if (item.normalizationfunction == "inverse") {
      percent = 100 - percent;
    }
    return percent;
  }

  render() {
    return <div className="field">
        {this.renderField(this.props.item, this.props.value)}
        <span onClick={(e) => this.toggleInformation()} className="informationButton"><i class="bi bi-info-circle-fill"></i></span>
        <div className={"information " + (this.state.showInformation ? "" : "hidden")}>
          {this.props.item.description}<br/>
          <a target="_blank" href={this.props.item.link}>{this.props.item.link}</a>
        </div>
      </div>
  }

  renderField(item, value) {
    if (item.displaytype == "prettyampersand") {
      return this.renderPrettyampersand(item, value);
    } else if (item.displaytype == "pubmed") {
      return this.renderPubmed(item, value);
    } else if (item.displaytype == "ccds") {
      return this.renderCcds(item, value);
    } else if (item.displaytype == "ensp") {
      return this.renderEnsp(item, value);
    } else if (item.displaytype == "uniprot") {
      return this.renderUniprot(item, value);
    } else if (item.displaytype == "uniparc") {
      return this.renderUniparc(item, value);
    } else if (item.displaytype == "impact") {
      return this.renderImpact(item, value);
    } else if (item.displaytype == "clinsig") {
      return this.renderClinsig(item, value);
    } else if (item.displaytype == "clinvaromims") {
      return this.renderClinvaromims(item, value);
    } else if (item.displaytype == "clinvarid") {
      return this.renderClinvarid(item, value);
    } else if (item.displaytype == "mastermind") {
      return this.renderMastermind(item, value);
    } else if (item.displaytype == "hgvsc") {
      return this.renderHgvsc(item, value);
    } else if (item.displaytype == "prettyampersandwithcosmic") {
      return this.renderPrettyampersandwithcosmic(item, value);
    }

    if (item.type == "bigint") {
      return <span className="fielddata">
          <span className="title">
            {item.name}
          </span>
          <span className="rightValue">
            {value}
          </span>
        </span>;
    } else if (item.type == "double precision") {
      let width = this.computePercentWidth(value, item);
      return <span className="fielddata">
          <span className="title">
            {item.name}
          </span>
          <span className="rightValue">
            <b>{Number(width).toFixed(0)}%</b> {Number(value).toFixed(5)} ({item.from} - {item.to})
            <span className="barBackground">
              <span style={{"width" : (width + "%")}} className={"bar " + item.normalizationfunction}></span>
            </span> 
            <span className="inverse">{item.normalizationfunction == "inverse" ? "This value was normalized to percent. Higher values correlate with more damaging mutations" : ""}</span>
          </span>
      </span>;
    } else {
      return <span className="fielddata">
          <span className="title">
            {item.name}
          </span>
          <span className="rightValue">
            {value}
          </span>
        </span>;
    }
  }

  expandDiv() {
    this.setState(prevState => ({ showInformation: prevState.showInformation, expanded: true }));
  }
 
  renderPrettyampersand(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          {value.split('&').map((listItem) => {
            if (listItem != ".") {  
              return <div className="listItem">{listItem.replaceAll("_", " ")}</div>
            }
          })}
        </span>
      </span>;    
  }

  renderPrettyampersandwithcosmic(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          {value.split('&').map((listItem) => {
            if (listItem != ".") {  
              if (listItem.startsWith("COSV")) {
                return <div className="listItem"><a target="_blank" href={"https://cancer.sanger.ac.uk/cosmic/search?q=" + listItem}>{listItem.replaceAll("_", " ")}</a></div>
              } else {
                return <div className="listItem">{listItem.replaceAll("_", " ")}</div>
              }
            }
          })}
        </span>
      </span>;    
  }

  renderPubmed(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <div style={{"maxHeight": ((this.state.expanded ? "100000" : "200") + "px")}} className="expansionDiv">
            {value.split('&').map((listItem) => { 
              return <div><a target="_blank" href={"https://pubmed.ncbi.nlm.nih.gov/" + listItem}>PubMed {listItem}</a></div>
            })}
          </div>
          {(value.split('&').length > 10 && !this.state.expanded) ? <a className="linkStyle" onClick={(e) => this.expandDiv()}>... expand list</a> : ""}
        </span>
      </span>;   
  }

  renderClinvaromims(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <div>
            {value.split('&').map((listItem) => { 
              return <div><a target="_blank" href={"https://www.omim.org/entry/" + listItem.replaceAll(/\D/g,'')}>Omim {listItem}</a></div>
            })}
          </div>
        </span>
      </span>;   
  }

  renderCcds(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <a target="_blank" href={"https://www.ncbi.nlm.nih.gov/projects/CCDS/CcdsBrowse.cgi?REQUEST=ALLFIELDS&DATA=" + value}>{value}</a>
        </span>
      </span>;   
  }  

  renderEnsp(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <a target="_blank" href={"http://www.ensembl.org/Homo_sapiens/Search/Results?q=" + value + ";site=ensembl;page=1;facet_species=Human"}>{value}</a>
        </span>
      </span>;   
  }

  renderUniprot(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <a target="_blank" href={"https://www.uniprot.org/uniprot/" + value.split(".")[0]}>{value}</a>
        </span>
      </span>;   
  }  

  renderUniparc(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <a target="_blank" href={"https://www.uniprot.org/uniparc/" + value.split(".")[0]}>{value}</a>
        </span>
      </span>;   
  } 

  renderClinvarid(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <a target="_blank" href={"https://www.ncbi.nlm.nih.gov/clinvar/variation/" + value}>{value}</a>
        </span>
      </span>;   
  } 

  renderImpact(item, value) {
    let color = "#ccc";
    if (value == "HIGH") {
      color = "#dc0000";
    } else if (value == "MODERATE") {
      color = "#dc8400";
    } else if (value == "LOW") {
      color = "#ffad00";
    } else {
      color = "#00a087";
    }

    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <span className="inlineBox" style={{"background": color}} >{value}</span>
        </span>
      </span>;   
  }

  renderClinsig(item, value) {
    let color = "#ccc";
    if (value == "HIGH") {
      color = "#dc0000";
    } else if (value == "MODERATE") {
      color = "#dc8400";
    } else if (value == "LOW") {
      color = "#ffad00";
    } else if (value == "benign") {
      color = "#00a087";
    }

    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          {value.split('&').map((listItem) => { 
            return <span className="inlineBox" style={{"background": color}} >{listItem.replaceAll("_", " ")}</span>
          })}
        </span>
      </span>;   
  }  

  renderMastermind(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <div>
            {value.split('&').map((listItem) => { 
              return <div><a target="_blank" href={"https://mastermind.genomenon.com/detail?mutation=" + listItem}>Mastermind {listItem}</a></div>
            })}
          </div>
        </span>
      </span>;   
  }  

  renderHgvsc(item, value) {
    return <span className="fielddata">
        <span className="title">
          {item.name}
        </span>
        <span className="rightValue">
          <a target="_blank" href={"https://www.lrg-sequence.org/search/?query=" + value.split(".")[0]}>{value}</a>
        </span>
      </span>;   
  }   

}

export default ViewField;
