import './View.css';
import React from 'react';  

import ViewField from './ViewField.jsx';

import Chart from "react-apexcharts";
import igv from 'igv'
//import igv from '../igv/igv.js'
//import igv from "https://cdn.jsdelivr.net/npm/igv@2.10.5/dist/igv.esm.min.js"

import Config from './config.js';

class View extends React.Component {

  constructor() {
    super();
    this.state = { variant: null, meta: null, view: null, error: null, panelVisibility: [], notes: [] };
    this.renderedFields = []; 
    this.igvRef = React.createRef();
  }

  componentDidMount() {
    this.fetchMeta();
    this.fetchView();
    this.fetchVariant();
    this.fetchNotes();
  }  

  componentDidUpdate() {
    this.renderIgv();
  }

  fetchVariant() {
    console.log("fetching");

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: this.props.token.tokenString, sample: this.props.matchProps.name, pid: this.props.matchProps.pid })
    };

    fetch(Config.apiBaseUrl + '/variant/loadsingle', requestOptions)
      .then(
        response => {
          if (response.status == 404) {
            this.setState(prevState => ({variant: prevState.variant, meta: prevState.meta, view: prevState.view, error: "Variant not found!", notes: prevState.notes}))
          }
          return response.json()
        }
      )
      .then(
        data => {
          this.setState(prevState => ({variant: data, meta: prevState.meta, error: prevState.error, panelVisibility: prevState.panelVisibility, notes: prevState.notes}))
        }
      );
  }

  fetchMeta() {
    console.log("fetching");

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: this.props.token.tokenString, sample: this.props.matchProps.name })
    };

    fetch(Config.apiBaseUrl + '/variant/loadmeta', requestOptions)
      .then(response => response.json())
      .then(
        data => {
          this.setState(prevState => ({variant: prevState.variant, meta: data, view: prevState.view, error: prevState.error, panelVisibility: prevState.panelVisibility, notes: prevState.notes}))
        }
      );
  }

  fetchView() {
    console.log("fetching");

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: this.props.token.tokenString, sample: this.props.matchProps.name })
    };

    fetch(Config.apiBaseUrl + '/variant/loadview', requestOptions)
      .then(response => response.json())
      .then(
        data => {
          this.setState(prevState => ({variant: prevState.variant, meta: prevState.meta, view: data, error: prevState.error, panelVisibility: prevState.panelVisibility, notes: prevState.notes}))
        }
      );
  }

  fetchNotes() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/note/get/' + this.props.matchProps.name + '/' + this.props.matchProps.pid, requestOptions)
      .then(response => response.json())
      .then(
        data => {
          console.log(data);
          this.setState(prevState => ({variant: prevState.variant, meta: prevState.meta, view: prevState.view, error: prevState.error, panelVisibility: prevState.panelVisibility, notes: data}))
        }
      );
  }

  getFormats(meta) {
    let result = [];

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

  getChartData(variant, meta) {
    if (variant == null || variant.info == null) {
      return [];
    }

    let scores = ["info_csq_sift4g_score", "info_csq_polyphen", "info_csq_dann_score", "info_csq_gerp_rs", "info_csq_fathmm_score", "info_csq_primateai_score", "info_csq_metasvm_score", "info_csq_revel_score", "info_csq_mvp_score"];

    let chart = {
      series: [],            
      options: {
        colors:['#e64b35', '#4dbbd5', '#00a087', '#3c5488', '#f39b7f', '#8491b4', '#91d1c2', '#dc0000', '#7e6148', '#b09c85'],
        chart: {
          type: 'polarArea',
        },        
        labels: [],
        stroke: {
          strokeWidth: 0,
          colors: ['#fff']
        },
        yaxis: {
          max: 100
        },
        tooltip: {
            y: {
              formatter: function(value, opts) {
                return value + '%'
              }
            }
        },        
        fill: {
          opacity: 0.9
        },
        legend: {
          position: 'bottom',
          labels: {
            useSeriesColors: true,
          },
        },   
        plotOptions: {
          polarArea: {
            rings: {
              strokeWidth: 1,
              strokeColor: '#eee',
            },
            spokes: {
              strokeWidth: 0,
              connectorColors: '#eee',
            }   
          }
        }
      }
    };   

    for (var i = 0; i < scores.length; i++) {
      if (variant.info[scores[i]] != null) {
        let scoreMeta = this.getMeta(scores[i], meta);
        if (scoreMeta != null) {
          let normalized = 0;
          if (scores[i] == "info_csq_polyphen") {
            if (variant.info[scores[i]] == "benign") {
              normalized = 7.5;
            } else if (variant.info[scores[i]] == "possiblydamaging") {
              normalized = 50;
            } else if (variant.info[scores[i]] == "probablydamaging") {
              normalized = 92.5;
            }
          } else {
            normalized = Number(100 * (variant.info[scores[i]] - scoreMeta.from) / (scoreMeta.to - scoreMeta.from)).toFixed(0);
            if (scoreMeta.normalizationfunction == "inverse") {
              normalized = 100 - normalized;
            }
          }  
          chart.series.push(normalized);
          chart.options.labels.push(scoreMeta.name);
        }
      }
    }    

    return chart;
  }

  getColor(value){
    //value from 0 to 1
    let hue = ((1-value)*120).toString(10);
    return ["hsl(",hue,",100%,50%)"].join("");
  }  

  getMeta(id, meta) {
    for (var i = 0; i < meta.length; i++) {
      if (meta[i].id == id) {
        return meta[i];
      }
    }
    return null;
  }

  isActiveHashLink(hash) {
    if(window.location.hash) {
      var currentHash = window.location.hash.substring(1); //Puts hash in variable, and removes the # character
      alert(currentHash);
      return hash == currentHash;      
    }
    return false;
  }

  changeNote(e) {
    this.setState(prevState => ({ note: e.target.value }));
  }

  addNote(e) {
    let newNote = {
                    id: 0,
                    sampleId: 0,
                    sampleName: this.props.matchProps.name,
                    researcherId: this.props.token.user.id,
                    researcherName: this.props.token.user.email,
                    variantId: this.props.matchProps.pid,
                    variantPosition: this.state.variant.chrom + "-" + this.state.variant.pos + "-" + this.state.variant.ref + "-" + this.state.variant.alt,
                    note: this.state.note,
                    time: ""
                  };

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tokenString: this.props.token.tokenString,
          note: newNote
        })
    };

    fetch(Config.apiBaseUrl + '/note/create', requestOptions)

    this.setState(prevState => ({ note: "", notes: [...this.state.notes, newNote]}));
  }

  deleteNote(id) {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/note/delete/' + id, requestOptions);

    this.setState(prevState => ({ notes: this.state.notes.filter(n => n.id != id) }))
  }

  renderTableOfContents() {
    if (this.state.view == null) {
      return "";
    }

    return <div className="sticky-top">
        <a href={"#"}><i>Top</i></a><br/>
        {this.state.view.chapters.map((item, index) => {return this.renderTableOfContentsRecursive(item, index + 1, 0)})}
      </div>;    
  }

  renderTableOfContentsRecursive(chapter, positionPrefix, depth) {
    return <div style={{"padding-left": (10 * depth + "px")}}>
        <a className={"tableOfContentsLink " + this.isActiveHashLink(positionPrefix) ? "active" : ""} href={"#" + positionPrefix}>{positionPrefix} {chapter.title}</a>
        {chapter.subchapters.map((item, index) => {return this.renderTableOfContentsRecursive(item, positionPrefix + "." + (index + 1), depth + 1)})}
      </div>;
  }

  renderContents() {
    if (this.state.view == null) {
      return "";
    }

    return <div>
        {this.state.view.chapters.map((item, index) => {return this.renderContentsRecursive(item, index + 1, 0)})}
      </div>;    
  }

  renderContentsRecursive(chapter, positionPrefix, depth) {
    let externalDataBoxes = [{name: "ClinVar", logo: "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/US-NLM-NCBI-Logo.svg/1280px-US-NLM-NCBI-Logo.svg.png"}];

    let isExternalDataBox = externalDataBoxes.map(d => d.name).includes(chapter.title);
    let numberOfChildren = chapter.fields.filter(x => this.state.variant.info[x] != null).length;

    return <div>
        <div className="contentHeading" id={positionPrefix} style={{"font-size": ((24 - 4 * depth) + "px")}}>{positionPrefix} {chapter.title}</div>
        <div className={externalDataBoxes.map(d => d.name).includes(chapter.title) ? "externalBoxData" : ""}>
        {isExternalDataBox && numberOfChildren != 0 ? <div><span className="externalBoxDataTitle">{this.state.variant.chrom}-{this.state.variant.pos}-{this.state.variant.ref}-{this.state.variant.alt}</span><hr/></div> : ""}
        {isExternalDataBox ? <img className="externalLogo" src={externalDataBoxes.find(d => d.name == chapter.title).logo}/> : ""}
        {isExternalDataBox && numberOfChildren == 0 ? "This entity was not found in " + chapter.title : ""}
        {
          chapter.fields.map((item) => {
            return this.renderField(this.getMeta(item, this.state.meta))
          })
        }
        </div>
        <br/>
        {chapter.subchapters.map((item, index) => {return this.renderContentsRecursive(item, positionPrefix + "." + (index + 1), depth + 1)})}
        {
          depth == 0 ? <hr/> : ""
        }
      </div>;
  }  

  renderField(item) {
    if (item == null) {
      return;
    }

    let value = this.state.variant.info[item.id];

    if (!item.displayable || value == null) {
      return;
    }

    this.renderedFields.push(item.id);

    let enhancedInformation = <span></span>;
    console.log(this.state.variant.indexedGeneDTO != null && item.id == "info_csq_symbol");
    if (item.id == "info_csq_symbol") {
      enhancedInformation = this.renderGeneData(this.state.variant.indexedGeneDTOs);
    }

    return <span>
        <ViewField item={item} value={value} />
        {enhancedInformation}
      </span>;
  }

  renderChart() {
    let chart = this.getChartData(this.state.variant, this.state.meta);
    if (chart.series.length == 0) {
      return "";
    }
    return <Chart options={chart.options} series={chart.series} type="polarArea" />
  }

  renderAlleleFrequencies() {
    let frequencies = ["info_csq_gnomad_genomes_af", "info_csq_gnomad_genomes_afr_af", "info_csq_gnomad_genomes_ami_af", "info_csq_gnomad_genomes_amr_af", "info_csq_gnomad_genomes_asj_af", "info_csq_gnomad_genomes_eas_af", "info_csq_gnomad_genomes_fin_af", "info_csq_gnomad_genomes_nfe_af", "info_csq_gnomad_genomes_sas_af", "info_csq_1000gp3_af", "info_csq_1000gp3_afr_af", "info_csq_1000gp3_amr_af", "info_csq_1000gp3_eas_af", "info_csq_1000gp3_eur_af", "info_csq_1000gp3_sas_af", "info_csq_gnomad_af", "info_csq_gnomad_afr_af", "info_csq_gnomad_amr_af", "info_csq_gnomad_asj_af", "info_csq_gnomad_eas_af", "info_csq_gnomad_fin_af", "info_csq_gnomad_nfe_af", "info_csq_gnomad_oth_af", "info_csq_gnomad_sas_af", "info_csq_gnomadg_af","info_csq_gnomadg_controls_af","info_csq_gnomadg_af_afr","info_csq_gnomadg_af_amr","info_csq_gnomadg_af_asj","info_csq_gnomadg_af_eas","info_csq_gnomadg_af_fin", "info_csq_gnomadg_af_nfe","info_csq_gnomadg_af_oth"];

    let pairs = [];
    for (var i = 0; i < frequencies.length; i++) {
      let name = frequencies[i];
      let meta = this.getMeta(frequencies[i], this.state.meta);
      if (meta != null) {
        name = meta.name;
      }

      let value = this.state.variant.info[frequencies[i]];
      if (value != null) {
        pairs.push({name: name, value: value, description: meta?.description});
      }
    }

    let sorted = pairs.sort((a,b) => b.value - a.value).slice(0, 10);

    if (sorted.length == 0) {
      return "";
    }

    return <div className="alleleFrequencies">
        {sorted.map((item) => {
          return <div> 
              <div className="graphBarText" title={item.description}>
                <b>{item.name}</b>
                <div className="rightAlignClear">
                  {Number(item.value).toFixed(4)}
                </div>
              </div>               
              <div className="graphBarBackground">         
                <div className="graphBar" style={{"width": (100 * item.value + "%")}}></div>
              </div>
            </div>
        })}      
        <span onClick={(e) => this.toggleAlleleFrequencyInformation()} className="informationButton informationButtonRight"><i class="bi bi-info-circle-fill"></i></span>
        <div className={"information " + (this.state.showAlleleFrequencyInformation ? "" : "hidden")}>
          This list shows the 10 highest allele frequencies among all available allele frequencies.
        </div>
      </div>
  }

  renderFilter() {
    if (this.state.variant.filter != null && this.state.variant.filter.toLowerCase() == "pass") {
      return <span className="inlineBox" style={{background: "#00a087"}}>Pass</span>
    } else {
      return <span><span className="inlineBox" style={{background: "#dc0000"}}>Fail</span>{this.state.variant.filter}</span>;
    }
  }  

  getLocalization() {
    let chrom = this.state.variant.chrom.replace(/\D/g,'');
    if (this.state.variant.chrom == "chrX") {
      chrom = "X";
    } else if (this.state.variant.chrom == "chrY") {
      chrom = "Y";
    }
    return chrom + "-" + this.state.variant.pos + "-" + this.state.variant.ref + "-" + this.state.variant.alt
  }

  toggleInformation() {
    this.setState(prevState => ({ showInformation: !prevState.showInformation }));
  }

  toggleAlleleFrequencyInformation() {
    this.setState(prevState => ({ showAlleleFrequencyInformation: !prevState.showAlleleFrequencyInformation }));
  }


  renderIgv() {
    var igvDiv = this.igvRef.current;
    if (igvDiv == null || this.igvRendered) {
      return;
    }

    console.log(this.state.variant.igvUrl);
    console.log(this.state.variant.igvIndexUrl);

    var options =
      {
          genome: "hg38",
          locus: (this.state.variant.chrom + ":" + this.state.variant.pos),
          tracks: [
              {
                  "name": this.props.matchProps.name,
                  "url": Config.apiBaseUrl + this.state.variant.igvUrl,
                  "indexURL": Config.apiBaseUrl + this.state.variant.igvIndexUrl,
                  "format": this.state.variant.igvFormat //cram or bam
              }
          ]
      };

    igv.createBrowser(igvDiv, options)
    .then(function (browser) {
        console.log("Created IGV browser");
    });

    this.igvRendered = true;
  }

  render() {
    if (this.state.error != null) {
      return <b>{this.state.error}</b>
    }

    if (this.state.variant == null || this.state.meta == null || typeof this.state.variant.info == 'undefined') {
      return <div className="loading">
          <div class="spinner-border spinner-border" role="status"></div><br/>Loading variant...
        </div>;
    }

    console.log(this.state.variant);
    console.log(this.state.meta);

    this.renderedFields = [];

    return <div className="center overflowhidden">
        {this.state.notes && this.state.notes.map(n => {
            return(
              <div className="singleNote">
                <div className="noteHeader">
                  <div className="inlineLeft">
                    <i style={{color: "orange"}} class="bi bi-star-fill"></i> <b><a href={"/view/" + n.sampleName + "/" + n.variantId}>{n.variantPosition}</a></b> | <a href={"/sample/" + n.sampleName}>{n.sampleName}</a>
                  </div>
                  <div className="inlineRight">
                    {new Date(n.time).toLocaleDateString()} {new Date(n.time).toLocaleTimeString()} | <i onClick={(e) => this.deleteNote(n.id)} style={{cursor: "pointer"}} class="bi bi-trash"></i>
                  </div>
                </div>
                <div className="noteBody">
                  <b>{n.researcherName}</b>:
                  <div className="noteText">
                    <i>"{n.note}"</i>
                  </div>
                </div>
              </div>
            );
        })}
        <div className="noteAdder">
          <i style={{color: "orange", "font-size": "19px"}} class="bi bi-star"></i>
          <input onChange={(e) => this.changeNote(e)} value={this.state.note} placeholder="Add a note to this variant to find it later..." /> <button onClick={(e) => this.addNote(e)} className="sec">Save</button>
        </div>
        <div className="halfinlineblockleft">
          <table className="top">
            <tr><td className="key">Sample</td><td className="value">{this.props.matchProps.name}</td></tr>
            <tr><td className="key">Chromosome</td><td className="value">{this.state.variant.chrom}</td></tr>
            <tr><td className="key">Position</td><td className="value">{this.state.variant.pos}</td></tr>
            <tr><td className="key">Reference bases</td><td className="value">{this.state.variant.ref}</td></tr>
            <tr><td className="key">Alternative bases</td><td className="value">{this.state.variant.alt}</td></tr>
            <tr><td className="key">Filter</td><td className="value">{this.renderFilter()}</td></tr>
          </table>
        </div>
        <div className="halfinlineblockright">
          <table className="top">
            <tr><td className="key"></td><td className="key">GT</td><td className="key">GQ</td><td className="key">DP</td><td className="key">AD</td></tr>
            {this.getFormats(this.state.meta).map((item, index) => {
              if (this.state.variant != null && this.state.variant.info != null) {
                return <tr><td className="value">{item}</td><td className="value">{this.state.variant.info["format_" + item + "_gt"]}</td><td className="value">{this.state.variant.info["format_" + item + "_gq"]}</td><td className="value">{this.state.variant.info["format_" + item + "_dp"]}</td><td className="value">{this.state.variant.info["format_" + item + "_ad"].replaceAll(",", "/")}</td></tr>
              }
            })}
          </table>        
        </div>

        <div className="digest">
          <table className="top">
            <tr>
              <td>HGVSC description</td>
              <td>Gene</td>
              <td>Variant class</td>
              <td>Impact</td>
              <td>Consqeuence</td>
            </tr>
            <tr>
              <td className="value">{this.state.variant.info["info_csq_hgvsc"]}</td>
              <td className="value"><a target="_blank" href={"https://www.ncbi.nlm.nih.gov/gene/?term=" + this.state.variant.info["info_csq_gene"]}>{this.state.variant.info["info_csq_gene"]} ({this.state.variant.info["info_csq_symbol"]})</a></td>
              <td className="value">{this.state.variant.info["info_csq_variant_class"]}</td>
              <td className="value">
                {this.renderDigestImpact(this.state.variant.info["info_csq_impact"])}
                <span onClick={(e) => this.toggleInformation()} className="informationButton"><i class="bi bi-info-circle-fill"></i></span>
              </td>
              <td className="value" style={{wordBreak: "break-word"}}>{this.state.variant.info["info_csq_consequence"] != null ? this.state.variant.info["info_csq_consequence"].split("&").map((item) => {return item + " "}) : ""}</td>
            </tr>
            <tr className={"information " + (this.state.showInformation ? "" : "hidden")}>
              <td colspan="5">
                {this.getMeta("info_csq_impact", this.state.meta).description}<br/>
                <a target="_blank" href={this.getMeta("info_csq_impact", this.state.meta).link}>{this.getMeta("info_csq_impact", this.state.meta).link}</a>
              </td>
            </tr>
            <tr>
              <td colspan="5">
                <a href={"https://gnomad.broadinstitute.org/variant/" + this.getLocalization() + "?dataset=gnomad_r3"} target="_blank">View this variant on gnomAd</a> | <a href={"https://bravo.sph.umich.edu/freeze8/hg38/variant/snv/" + this.getLocalization()} target="_blank">View this variant on TopMed Bravo</a> | <a target="_blank" href={"https://www.lrg-sequence.org/search/?query=" + this.state.variant?.info["info_csq_hgvsc"]?.split(".")[0]}>View this transcript on LRG</a> | <a href={"https://franklin.genoox.com/clinical-db/variant/snp/" + this.state.variant.chrom + "-" + this.state.variant.pos + "-" + this.state.variant.ref + "-" + this.state.variant.alt} target={"_blank"}>View this variant on Franklin</a> | <a href={"https://genome-euro.ucsc.edu/cgi-bin/hgTracks?db=hg38&lastVirtModeType=default&lastVirtModeExtraState=&virtModeType=default&virtMode=0&nonVirtPosition=&position=" + this.state.variant.chrom + ":" + this.state.variant.pos} target="_blank">View this on UCSC</a> | <a href={"https://varsome.com/variant/hg38/" + this.getLocalization() + "?annotation-mode=germline"} target="_blank">View this on Varsome</a> {this.renderClinvarLink()}
              </td>
            </tr>
          </table>
        </div>

        <div className="plot overflowhidden">
          <div className="halfinlineblockleft">
            {this.renderChart()}
          </div>
          <div className="halfinlineblockright">
            {this.renderAlleleFrequencies()}
          </div>
        </div>

        <br/>

        <div className="tableOfContents">
          {this.renderTableOfContents()}
        </div>

        <hr/>
        <div ref={this.igvRef}></div><br/>
        <hr/>

        {this.renderIsoforms()}<br/>

        {this.renderContents()}<br/>

        <div className="contentHeading otherFields">
          Other fields
        </div>
        {this.state.meta.map((item, index) => {
          return (this.renderedFields.includes(item.id) ? "" : this.renderField(item))
        })}
      </div>;
  }

  renderClinvarLink() {
    let id = this.state.variant.info["info_csq_clinvar"];
    if (id != null) {
        return <span> | <a target="_blank" href={"https://www.ncbi.nlm.nih.gov/clinvar/variation/" + id}>View this variant on ClinVar</a></span>;
    }

    return "";
  }

  renderIsoforms() {
    if (this.state.variant.isoforms == null || this.state.variant.isoforms.length <= 1) {
      return "";
    }

    return <div>
        <b>You are viewing Isoform {this.state.variant.isoforms.map(i => i.pid + "").indexOf(this.props.matchProps.pid) + 1}.</b> Other Isoforms: 
        {this.state.variant.isoforms.map((item, index) => {
          if (item.pid == this.props.matchProps.pid) {
            return "";
          }
          return <span><a href={"/view/" + this.props.matchProps.name + "/" + item.pid} target="_blank">{index + 1} ({item.info["info_csq_feature"]})</a> </span>;
        })} 
      </div>;
  }

  togglePanelVisibility(i) {
    let newPanelVisibility = this.state.panelVisibility;
    if (newPanelVisibility[i] != null) {
      newPanelVisibility[i] = null;
    } else {
      newPanelVisibility[i] = true;
    }
    this.setState(prevState => ({variant: prevState.variant, meta: prevState.meta, error: prevState.error, panelVisibility: newPanelVisibility, notes: prevState.notes}))
  }

  renderGeneData(indexedGeneDTOs) {
    if (indexedGeneDTOs == null || indexedGeneDTOs.length == 0) {
      return <div className="externalBoxData">This entity is not part of any Genomics England panels <img className="externalLogo" src="https://upload.wikimedia.org/wikipedia/en/f/f2/Genomics_England_logo.svg"/></div>;
    }

    return <div className="externalBoxData">
              <img className="externalLogo" src="https://upload.wikimedia.org/wikipedia/en/f/f2/Genomics_England_logo.svg"/>
              <div className="info"></div>
              <div className="fullGeneNameDetail">
                {indexedGeneDTOs[0].symbol} - {indexedGeneDTOs[0].name}
              </div>
              {indexedGeneDTOs.map((indexedGeneDTO, i) => {
                return <div>
                    <div onClick={(e) => this.togglePanelVisibility(i)} className="fullPanelNameDetail">
                      <i className={"glyphicon bi bi-caret-" + (this.state.panelVisibility[i] ? "down" : "right") + "-fill"} ></i> {this.renderConfidenceLevel(indexedGeneDTO.confidenceLevel)} {indexedGeneDTO.panelNames[0]} <span className="note">click to view details</span>
                    </div>
                    <div style={{display: (this.state.panelVisibility[i] ? "block" : "none")}} className="singlePanelGeneData">
                      <a target="_blank" href={"https://www.genenames.org/data/gene-symbol-report/#!/hgnc_id/" + indexedGeneDTOs[0].hgncId}>{indexedGeneDTOs[0].hgncId}</a>
                      {indexedGeneDTOs[0].omim != null ? indexedGeneDTOs[0].omim.map((item, index) => {
                        return <div><a target="_blank" href={"https://www.omim.org/entry/" + item}>{indexedGeneDTOs[0].symbol} on Omim</a></div>;
                      }) : ""}
                      {(indexedGeneDTO.penetrance != null && indexedGeneDTO.penetrance != "" ? <div><b>Penetrance:</b> {indexedGeneDTO.penetrance}</div> : "")}
                      {(indexedGeneDTO.modeOfPathogenicity != null && indexedGeneDTO.modeOfPathogenicity != "" ? <div><b>Mode of pathogenicity:</b> {indexedGeneDTO.modeOfPathogenicity}</div> : "")}
                      {(indexedGeneDTO.modeOfInheritance != null && indexedGeneDTO.modeOfInheritance != "" ? <div><b>Mode of inheritance:</b> {indexedGeneDTO.modeOfInheritance}</div> : "")}
                      <div><b>Confidence level:</b> {indexedGeneDTO.confidenceLevel}</div>
                      <br/>
                      <div>
                        {indexedGeneDTO.publications != null ? indexedGeneDTO.publications.map((item, index) => {
                          return <div><a target="_blank" href={"https://pubmed.ncbi.nlm.nih.gov/" + item}>PubMed {item}</a></div>;
                        }) : ""}
                        <br/>
                      </div>

                      {
                        (indexedGeneDTO.phenotypes != null && indexedGeneDTO.phenotypes.length != 0) ? 
                        <div>
                          <b>Phenotypes:</b>
                          <ul>
                          {indexedGeneDTO.phenotypes.map((item, index) => {
                            return <li>{item}</li>;
                          })} 
                          </ul>
                        </div>
                        : ""
                      }

                      {
                        (indexedGeneDTO.evidence != null && indexedGeneDTO.evidence.length != 0) ? 
                        <div>
                          <b>Evidence:</b>
                          <ul>
                          {indexedGeneDTO.evidence.map((item, index) => {
                            return <li>{item}</li>;
                          })} 
                          </ul>
                        </div>
                        : ""
                      }
                  </div>                      
                  <hr/>
                  </div>
              })}
      </div>;
  }

  renderConfidenceLevel(value) {
    let color = "#ccc";
    let name = "";
    if (value <= 1) {
      color = "#dc0000";
      name = "RED";
    } else if (value > 1 && value < 3) {
      color = "#dc8400";
      name = "AMBER";
    } else if (value >= 3) {
      color = "#00a087";
      name = "GREEN";
    }

    return <span className="inlineBox" style={{"background": color}} >{name}</span>
  }  

  renderDigestImpact(value) {
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

    return <span className="inlineBox" style={{"background": color}} >{value}</span>
  }  

}

export default View;
