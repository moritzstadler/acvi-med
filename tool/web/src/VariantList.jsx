import './VariantList.css';

import Filter from './Filter.jsx';

import React from 'react';

import Config from './config.js';

class VariantList extends React.Component {

  scrollViewRefs = [];

  constructor() {
    super();
    
    let tutorial = {card: 0, message: {title: "Welcome!", description: "Click 'Continue' to perform the tutorial or skip to start working immediately. You can restart the tutorial at any later time.", style: {outline: "10000px solid #000000aa"}}, circle: {style: {display: "none"}}};
    if (localStorage.getItem("skiptutorial") != undefined) {
      tutorial = null;
    }

    this.state = {variants: [], queryData: null, meta: [], tutorial: tutorial, selected: null, cursor: -1, error: null, loading: false, panelIndex: null };

    this.page = 0;

    //refs for tutorial
    this.filterRef = React.createRef();

    //default filter for showing an empty starting page
    this.filter = { offset: 0, order: [], expression: { basic: false, operators: [], children: [{basic: true, name: "filter", comparator: "IN", values: ["PASS"]}] } };
    /*this.filter = {
                    "offset": 0,
                    "order": [
                        {
                            "name": "info_csq_dann_score",
                            "ascending": false
                        }
                    ],
                    "expression": {
                        "basic": false,
                        "operators": [],
                        "children": [
                            {
                                "basic": false,
                                "operators": [
                                    "AND"
                                ],
                                "children": [
                                    {
                                        "basic": false,
                                        "operators": [
                                            "OR",
                                            "OR",
                                            "OR",
                                            "OR",
                                            "OR"
                                        ],
                                        "children": [
                                            {
                                                "basic": true,
                                                "name": "info_csq_symbol",
                                                "comparator": "IN",
                                                "values": [
                                                    "APOB"
                                                ]
                                            },
                                            {
                                                "basic": true,
                                                "name": "info_csq_symbol",
                                                "comparator": "IN",
                                                "values": [
                                                    "MAPT"
                                                ]
                                            },
                                            {
                                                "basic": true,
                                                "name": "info_csq_symbol",
                                                "comparator": "IN",
                                                "values": [
                                                    "TNXB"
                                                ]
                                            },
                                            {
                                                "basic": true,
                                                "name": "info_csq_symbol",
                                                "comparator": "IN",
                                                "values": [
                                                    "TTN"
                                                ]
                                            },
                                            {
                                                "basic": true,
                                                "name": "info_csq_symbol",
                                                "comparator": "IN",
                                                "values": [
                                                    "DST"
                                                ]
                                            },
                                            {
                                                "basic": true,
                                                "name": "info_csq_symbol",
                                                "comparator": "IN",
                                                "values": [
                                                    "ADAMTS18"
                                                ]
                                            }
                                        ]
                                    },
                                    {
                                        "basic": true,
                                        "name": "info_csq_dann_score",
                                        "comparator": ">",
                                        "value": 0.000134
                                    }
                                ]
                            }
                        ]
                    }
                };*/
  }

  fetchVariants() {
    this.loading = true;
    this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, skip: prevState.skip, take: prevState.take, selected: prevState.selected, cursor: prevState.cursor, error: prevState.error, loading: true, popupFilterVisibleClass: ""}))

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: this.props.token.tokenString, sample: this.props.matchProps.name, page: this.page, filter: this.filter })
    };
    fetch(Config.apiBaseUrl + '/variant/load', requestOptions)
      .then(response => response.json())
      .then(
        data => {
          /*for (var i = 0; i < data.variants.length; i++) {
            this.scrollViewRefs.push(React.createRef());
          }*/

          console.log(data);
          this.endReached = false;

          let variants = data.variants;
          let queryData = data;
          queryData.variants = null;

          if (variants == null) {
            return;
          }

          //check if we have seen the entire result
          if (variants.length <= 0/* && (this.state.variants.length >= this.state.page * 100 || this.state.variants.length % 100 != 0)*/) {
            this.endReached = true;
          }

          this.getFilteredInfoFields();
          this.setState(prevState => ({variants: prevState.variants.concat(variants), queryData: queryData, meta: prevState.meta, panelIndex: prevState.panelIndex, tutorial: prevState.tutorial, selected: prevState.selected, cursor: prevState.cursor, error: null, loading: false}))
          this.loading = false;
        },
        error => {
          this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, panelIndex: prevState.panelIndex, tutorial: prevState.tutorial, selected: prevState.selected, cursor: prevState.cursor, error: JSON.stringify(error), loading: false}))
        }
      );

  }

  fetchPanelIndex() {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: this.props.token.tokenString })
    };

    fetch(Config.apiBaseUrl + '/knowledgebase/panelapp/getindex', requestOptions)
      .then(
        response => {
          return response.json()
        }
      )
      .then(
        data => {
          this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, panelIndex: data, tutorial: prevState.tutorial, selected: prevState.selected, cursor: prevState.cursor, error: null, loading: false}))
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
          this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: data, panelIndex: data, tutorial: prevState.tutorial, selected: prevState.selected, cursor: prevState.cursor, error: null, loading: false}))
        }
      );
  }

  /**
   * returns the ids and names of info fields included in the filter chosen by the user
   */
  getFilteredInfoFields() {
    if (this.filter == null) {
      return [];
    }

    var excludedInfoFields = ["info_csq_symbol", "info_controls_af_popmax", "filter", "info_csq_canonical", "info_csq_impact", "info_csq_consequence"];

    var infoFields = this.getFilteredInfoFieldsRecursive(this.filter.expression);
    var uniqueInfoFields = [];

    for (var i = 0; i < infoFields.length; i++) {
      if (!uniqueInfoFields.includes(infoFields[i]) && !(excludedInfoFields.includes(infoFields[i]) || (infoFields[i].startsWith("format_") && infoFields[i].endsWith("_gt")))) {
        uniqueInfoFields.push(infoFields[i]);
      }
    }

    for (var i = 0; i < this.filter.order.length; i++) {
      if (!uniqueInfoFields.includes(this.filter.order[i].name) && !excludedInfoFields.includes(this.filter.order[i].name)) {  
        uniqueInfoFields.push(this.filter.order[i].name);
      }
    }

    return uniqueInfoFields;
  }


  getFilteredInfoFieldsRecursive(expression) {
    if (expression == null) {
      return [];
    }
    var ids = [];
    for (var i = 0; i < expression.children.length; i++) {
      if (expression.children[i].basic) {
        ids.push(expression.children[i].name);
      } else {
        ids = ids.concat(this.getFilteredInfoFieldsRecursive(expression.children[i]));
      }
    }
    return ids;
  }  

  /**
   * returns meta data for a given id
   */
  getMeta(id) {
    if (this.state.meta == null) {
      return null;
    }

    for (var i = 0; i < this.state.meta.length; i++) {
      if (this.state.meta[i].id == id) {
        return this.state.meta[i];
      }
    }

    return null;
  }

  componentDidMount() {
    this.fetchMeta();
    this.fetchVariants();
    this.fetchPanelIndex();
  }

  select(index) {
    this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, tutorial: prevState.tutorial, selected: prevState.variants[index], cursor: index, error: prevState.error, loading: prevState.loading}));

    let selected = this.state.variants[index];
    const newWindow = window.open("/view/" + this.props.matchProps.name + "/" + selected.pid, "_blank", "noopener,noreferrer");
    if (newWindow) newWindow.opener = null
  }

  handleScroll = (e) => {
    if (this.endReached) {
      return;
    }

    const bottom = e.target.scrollHeight - e.target.scrollTop < e.target.clientHeight + 1000;
    if (bottom && !this.loading) {
      //this.setState(prevState => ({variants: prevState.variants, skip: prevState.skip + this.state.take + 1, take: prevState.take, selected: prevState.selected, cursor: prevState.cursor}));
      if (this.state.variants.length > 0) {
        this.state.page = this.state.page + 1;
        this.page = this.page + 1;
      }
      
      this.fetchVariants();
    }
  }

  handleKeyDown = (e) => {
    /*let newCursor = this.state.cursor;
    if (e.key === "ArrowDown") {
      newCursor = Math.min(Math.max(this.state.cursor + 1, 0), this.state.variants.length - 1);
    } else if (e.key === "ArrowUp") {
      newCursor = Math.min(Math.max(this.state.cursor - 1, 0), this.state.variants.length - 1);
    }

    if (newCursor == this.state.variants.length - 1) {
      this.state.skip = this.state.skip + this.state.take + 1;
      this.fetchVariants();
    }

    this.select(newCursor);
    this.scrollViewRefs[newCursor].current.scrollIntoView({ block: 'end' });
    //this.scrollViewRef.current.scrollTo(0, this.scrollViewRowRef.current.clientHeight * (this.state.cursor + 3) - this.scrollViewRef.current.clientHeight);
    */
  }

  filterApplied = (filter) => {
    console.log(filter);

    this.page = 0;
    this.closeFilter();
    this.filter = filter;
    this.endReached = false;
    this.setState(prevState => ({variants: [], queryData: null, meta: prevState.meta, tutorial: prevState.tutorial, selected: null, cursor: prevState.cursor, error: prevState.error, loading: true, popupFilterVisibleClass: ""}));
    this.fetchVariants();
  }

  openFilter() {
    this.fetchPanelIndex();
    this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, tutorial: prevState.tutorial, selected: prevState.selected, cursor: prevState.cursor, error: prevState.error, loading: prevState.loading, popupFilterVisibleClass: "visible"}));
  }

  closeFilter() {
    this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, tutorial: prevState.tutorial, selected: prevState.selected, cursor: prevState.cursor, error: prevState.error, loading: prevState.loading, popupFilterVisibleClass: ""}));
  }  

  getHeatMap(variant, meta) {
    if (variant == null || variant.info == null || meta.length == 0) {
      return [];
    }

    let heatMap = ["info_cadd_phred", "info_cadd_raw", "info_caddind_phred", "info_caddind_raw", "info_csq_sift4g_score", "info_csq_polyphen", "info_csq_dann_score", "info_csq_gerp_rs", "info_csq_fathmm_score", "info_csq_primateai_score", "info_csq_metasvm_score", "info_csq_revel_score", "info_csq_mvp_score"]
    
    let result = [];

    for (var i = 0; i < heatMap.length; i++) {
      let fieldMeta = this.getMeta(heatMap[i], meta);
      let value = ".";
      if (variant.info[heatMap[i]] != null) {
        if (heatMap[i] == "info_csq_polyphen") {
          if (variant.info[heatMap[i]] == "benign") {
            value = 0.075;
          } else if (variant.info[heatMap[i]] == "possiblydamaging") {
            value = 0.5;
          } else if (variant.info[heatMap[i]] == "probablydamaging") {
            value = 0.925;
          }
        } else {
          value = (variant.info[heatMap[i]] - fieldMeta.from) / (fieldMeta.to - fieldMeta.from);
          if (fieldMeta.normalizationfunction == "inverse") {
            value = 1 - value;
          }
        }
      }
      let name = "";
      if (fieldMeta != null) {
        name = fieldMeta.name;
      }
      result.push({
        name: name, 
        value: value 
      });
    }

    return result;
  }

  getColor(value){
    //value from 0 to 1
    let hue = ((1-value)*120).toString(10);
    return ["hsl(",hue,",100%,50%)"].join("");
  }    

  renderCanonical(item) {
    if (item.info.info_csq_canonical != null && item.info.info_csq_canonical.toLowerCase() == "yes") {
      return <span className="smallInlineBox" style={{background: "#00a087"}}>Yes</span>
    } else if (item.info.info_csq_tsl != null) {
      return <span className="smallInlineBox" style={{background: "#ccc"}}>TSL {item.info.info_csq_tsl}</span>;
    } else {
      return "";
    }
  }

  renderFilter(item) {
    if (item.filter != null && item.filter.toLowerCase() == "pass") {
      return <span className="smallInlineBox" style={{background: "#00a087"}}>Pass</span>
    } else {
      return <span title={item.filter} className="smallInlineBox" style={{background: "#dc0000"}}>Fail</span>;
    }
  }

  renderImpact(item) {
    let value = item.info.info_csq_impact;

    if (value == null || value == "") {
      return "";
    }

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

    return <span className="smallInlineBox" style={{background: color}}>{value}</span>
  }  

  getRowBackground(vid) {
    if (this.lastVid != vid) {
      if (this.rowBackground == "#fff") {
        this.rowBackground = "#f5f5f5";
      } else {
        this.rowBackground = "#fff";
      }
    }
    this.lastVid = vid;
    return this.rowBackground;
  }

  renderQueryData() {
    if (this.state.queryData == null) {
      return <span><div class="spinner-border spinner-border-sm" role="status"></div> Loading...</span>
    }

    let resultCount = this.state.queryData.resultCount;
    if (resultCount == this.state.queryData.resultCountLimit) {
      resultCount = Number(resultCount).toLocaleString() + "+";
    } else {
      resultCount = Number(resultCount).toLocaleString();
    }

    return <span>
        <b>{resultCount}</b> out of ~<b>{Number(this.state.queryData.allVariantsCount).toLocaleString()}</b> rows found with current filter in <b>{Number(this.state.queryData.elapsedMilliseconds / 1000).toFixed(2)} seconds</b>
      </span>;
  }

  render() {
    let message = "";

    if (this.state.error != null) {
      message = <div>
                  <b>Error</b> Could not load data {this.state.error}
                </div>
    }

    if (this.state.queryData == null || this.state.loading) {
      message = <div className="loading">
          <div class="spinner-border spinner-border" role="status"></div><br/>Loading...
        </div>
    } 

    if (this.endReached) {
      message = <div className="end">End of result</div>
    }

    let tutorial = "";
    if (this.state.tutorial != null) {
      tutorial = <div>
          <div onClick={(e) => this.tutorialContinue()} style={this.state.tutorial?.circle?.style} className="tutorialCircle"></div>
          <div style={this.state.tutorial?.message?.style} className="tutorialMessage">
            <h1>{this.state.tutorial?.message?.title}</h1>
            <span className="larger">{this.state.tutorial?.message?.description}</span><br/><br/>
            <button onClick={(e) => this.tutorialSkip()} className="sec">Skip for now</button> 
            <button onClick={(e) => this.tutorialContinue()} className="sec">Continue</button><br/><br/>
            <a href="#" onClick={(e) => this.tutorialHide()}>Skip and don't show again</a>
          </div>
        </div>;      
    }

    return <div>
            {tutorial}
            <div tabIndex={-1} onKeyDown={this.handleKeyDown} className="nooutline">
              <div className="listHeader">
                <div className="listHeaderLeft">
                  <b>{this.props.matchProps.name}</b>
                </div>
                <div className="listHeaderRight">
                  {
                    this.filter.expression == null ? <span className="hint"><i class="bi bi-lightbulb-fill"></i> You are viewing all mutations - apply a filter to make sense of the data</span> : <span className="hint"><i class="bi bi-lightbulb-fill"></i> Click on a row below to view additional details</span>
                  }
                  <button className="prim" onClick={(e) => this.openFilter()} >Filter Variants!</button>
                </div>
                <div className="queryData">
                  {this.renderQueryData()}
                </div>
              </div>
              <div onScroll={this.handleScroll} className="VariantList">
                <div className="tableWrapper">
                  <div className="tableFixedHead">
                    <table className="list">
                      <thead>
                        <tr>
                          <th>Chrom</th>
                          <th>Pos</th>
                          <th>Ref</th>
                          <th>Alt</th>
                          <th>Filter</th>
                          <th>Canonical</th>
                          <th>Max allele frequency</th>
                          <th>Impact</th>
                          <th>Consequence</th>
                          <th>Predictions</th>
                          <th>Gene</th>
                          {
                            this.state.meta && this.state.meta?.map((item) => {
                              if (item.id != null && item.id.startsWith("format_") && item.id.endsWith("_gt")) {
                                return <th>{item.id.replace("format_", "").replace("_gt", "")}</th>
                              }
                            })
                          }                          
                          {
                            this.getFilteredInfoFields().map((item) => {
                              return <th>{this.getMeta(item)?.name}</th>
                            })
                          }
                        </tr>
                      </thead>
                      <tbody>
                        {this.state.variants.map((item, index) => {
                          return <tr ref={this.scrollViewRefs[index - 1]} className={"clickable " + (this.state.cursor == index ? "selected" : "")} style={{background: this.getRowBackground(item.vid)}} onClick={(e) => this.select(index)}>
                            <td>{item.chrom}</td>
                            <td>{item.pos}</td>
                            <td>{item.ref}</td>
                            <td>{item.alt}</td>
                            <td>{this.renderFilter(item)}</td>
                            <td>{this.renderCanonical(item)}</td>
                            <td>
                              <span className="smallBarBackground">
                                <span style={{"width" : ((100 * item?.info?.info_controls_af_popmax) + "%")}} className="smallBar"></span>
                              </span> 
                              <span>{Number(100 * item?.info?.info_controls_af_popmax).toFixed(0)}%</span>
                            </td>
                            <td>{this.renderImpact(item)}</td>
                            <td>
                              {item?.info?.info_csq_consequence?.split("&").map((item) => {return item + " "})}
                            </td>
                            <td>
                              {
                                this.getHeatMap(item, this.state.meta).map((item) => {
                                  return <span title={item.name + ": " + (item.value != "." ? (Number(100 * item.value).toFixed(3) + "% damaging") : "Not available")} className="heatSplashBackground"><span className="heatSplash" style={{"background" : this.getColor(item.value)}}></span></span>
                                })
                              }
                            </td>
                            <td>
                              {item.info.info_csq_symbol}
                            </td>
                            {
                              this.state.meta.map((metaitem) => {
                                if (metaitem.id.startsWith("format_") && metaitem.id.endsWith("_gt")) {
                                  return <td>{item.info?.[metaitem.id]}</td>
                                }
                              })
                            }                                  
                            {
                              this.getFilteredInfoFields().map((id) => {
                                return <td>
                                    {(isNaN(item.info?.[id]) || item.info?.[id] == null) ? item.info?.[id] : Number(item.info?.[id]).toFixed(5)}
                                  </td>
                              })
                            } 
                          </tr>
                        })}
                      </tbody>
                    </table>
                    {message}
                    <br/><br/><br/><br/><br/><br/><br/>
                  </div>
                </div>
              </div>
            </div>
            <div className={"popupBackground " + this.state.popupFilterVisibleClass}>
              <div className="popup">
                <div className="right">
                  <a href="#" onClick={(e) => this.closeFilter()} ><i className="closeButton bi bi-x"></i></a>
                </div>
                <Filter ref={this.filterRef} panelIndex={this.state.panelIndex} token={this.props.token} meta={this.state.meta} sample={this.props.matchProps.name} apply={this.filterApplied} />
              </div>
            </div>
          </div>
  }

  tutorialSkip() {
    this.filterRef.current.changePageTo(1);
    this.filterRef.current.convertTextToFilter("Variant_quality IN [ PASS ]");
    this.filterRef.current.generateTextview();    
    this.closeFilter();
    this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, tutorial: null, selected: prevState.selected, cursor: prevState.cursor, error: prevState.error, loading: prevState.loading, popupFilterVisibleClass: prevState.popupFilterVisibleClass}));  
  }

  tutorialHide() {
    localStorage.setItem("skiptutorial", true);
    this.tutorialSkip();
  }

  tutorialContinue() {
    let tutorial = this.state.tutorial;

    tutorial.card++;

    if (tutorial.card == 1) {
      tutorial.circle.style = {display: "none"};
      tutorial.message.style = {outline: "10000px solid #000000aa"};
      tutorial.message.title = "Tutorial";
      tutorial.message.description = "This tool helps you find potential causal variants in your sample and understand the impact of variants on the observed phenotype.";
    } else if (tutorial.card == 2) {
      tutorial.circle.style = {left: "80px", top: "200px", width: "500px", height: "500px"};
      tutorial.message.style = {};
      tutorial.message.title = "Variants";
      tutorial.message.description = "These are your variants. Depending on your current filter you might see a subset of all variants here.";
    } else if (tutorial.card == 3) {
      tutorial.circle.style = {right: "80px", top: "200px", width: "400px", height: "400px"};
      tutorial.message.style = {};
      tutorial.message.title = "Variants";
      tutorial.message.description = "Each row represents a variant. When you click on a row, you will be redirected to a page showing more detailed information about the variant.";
    } else if (tutorial.card == 4) {
      tutorial.circle.style = {right: "-20px", top: "-50px", width: "200px", height: "200px"};
      tutorial.message.style = {};
      tutorial.message.title = "Filtering";
      tutorial.message.description = "More than likely you do not want to view millions of variants at a time. You can view the variants that are important to you by applying a filter. Clicking on 'Filter Variants!' opens the filering page.";
    } else if (tutorial.card == 5) {
      this.openFilter();
      tutorial.circle.style = {left: "-100px", top: "-50px", width: "700px", height: "700px"};
      //tutorial.message.style = {right: "50px", top: "100px", left: "unset"};
      tutorial.message.title = "Filtering";
      tutorial.message.description = "This is the filtering page. In the following steps you will decide which subset of variants you want to view.";
    }  else if (tutorial.card == 6) {
      tutorial.circle.style = {left: "calc(5% - 10px)", top: "40px", width: "300px", height: "300px"};
      tutorial.message.style = {};
      tutorial.message.title = "Panels and genes";
      tutorial.message.description = "If you know the phenotype of your patient you can search for a panel of genes or even single genes. Let's try 'Melanoma'.";
    } else if (tutorial.card == 7) {
      this.filterRef.current.panelSelectorRef.current.search({target: {value: "melanoma"}});
      this.filterRef.current.panelSelectorRef.current.searchFieldRef.current.value = "melanoma";
      tutorial.circle.style = {left: "40%", top: "-50px", width: "700px", height: "700px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Panels and genes";
      tutorial.message.description = "After searching for 'Melanoma', detailed information will be shown on the right.";
    } else if (tutorial.card == 8) {
      this.filterRef.current.panelSelectorRef.current.search({target: {value: "melanoma"}});
      this.filterRef.current.panelSelectorRef.current.searchFieldRef.current.value = "melanoma";
      tutorial.circle.style = {right: "calc(5% - 10px)", top: "60px", width: "200px", height: "200px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Add the panel";
      tutorial.message.description = "If you are happy with the panel or gene you have found and want to filter your variants accordingly, click 'Add panel to filter' in the top right. You can apply multiple panels at the same time.";
    } else if (tutorial.card == 9) {
      this.filterRef.current.addPanelToFilter("FAMILIAL_MELANOMA");
      tutorial.circle.style = {left: "calc(5% - 30px)", top: "420px", width: "400px", height: "400px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "This is your filter";
      tutorial.message.description = "After clicking 'Add panel to filter' your panel appears in the bottom of the window.";
    } else if (tutorial.card == 10) {
      tutorial.circle.style = {right: "calc(5% - 10px)", top: "20px", width: "150px", height: "150px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Continue building your filter";
      tutorial.message.description = "If you are satisfied with the panels and genes you have added, you can continue by clicking 'Next'. You can also skip this step by clicking 'Next'.";
    } else if (tutorial.card == 11) {
      this.filterRef.current.changePage(1);
      tutorial.circle.style = {left: "-100px", top: "-50px", width: "700px", height: "700px"};
      tutorial.message.style = {};
      tutorial.message.title = "Choose variant properties";
      tutorial.message.description = "After clicking 'Next' you are on page 2. You can choose variant properties to further restrict your variants.";
    } else if (tutorial.card == 12) {
      tutorial.circle.style = {left: "calc(5% - 10px)", top: "50px", width: "300px", height: "300px"};
      tutorial.message.style = {};
      tutorial.message.title = "Choose variant properties";
      tutorial.message.description = "Let's try adding 'Impact according to Ensemble' to our filter! Click on the name of the property you want to select.";
    } else if (tutorial.card == 13) {
      this.filterRef.current.select(0);
      tutorial.circle.style = {left: "40%", top: "-50px", width: "700px", height: "700px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Choose variant properties";
      tutorial.message.description = "You will see more detailed information about the selected property on the right.";
    } else if (tutorial.card == 14) {
      tutorial.circle.style = {right: "calc(5% - 30px)", top: "60px", width: "200px", height: "200px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Add the property";
      tutorial.message.description = "If you are happy with the property you have selected and want to filter your variants accordingly, click 'Add to filter' in the top right.";
    } else if (tutorial.card == 15) {
      this.filterRef.current.addToFilter(this.filterRef.current.state.selected);
      tutorial.circle.style = {left: "calc(5% + 450px)", top: "450px", width: "400px", height: "400px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "This is your filter";
      tutorial.message.description = "After clicking 'Add to filter' the property you have selected appears in the bottom of the window.";
    } else if (tutorial.card == 16) {
      this.filterRef.current.convertTextToFilter("(Panel = FAMILIAL_MELANOMA) AND Variant_quality IN [ PASS ] AND Impact_according_to_Ensemble IN [ HIGH MODERATE ]");      
      tutorial.circle.style = {left: "calc(5% + 450px)", top: "450px", width: "400px", height: "400px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "This is your filter";
      tutorial.message.description = "By clicking the checkboxes you can choose the values this property can take.";
    } else if (tutorial.card == 17) {
      tutorial.circle.style = {left: "calc(5% + 450px)", top: "450px", width: "400px", height: "400px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "This is your filter";
      tutorial.message.description = "With this setting only variants with high or moderate impact are selected.";
    } else if (tutorial.card == 18) {
      tutorial.circle.style = {left: "calc(5% - 10px)", top: "50px", width: "300px", height: "300px"};
      tutorial.message.style = {};
      tutorial.message.title = "Add another variant property";
      tutorial.message.description = "Let's try adding 'GnomAD genomes AF' to our filter by clicking on it! This way only rare variants will be selected.";
    } else if (tutorial.card == 19) {
      this.filterRef.current.select(1);
      tutorial.circle.style = {left: "40%", top: "-50px", width: "700px", height: "700px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Add another variant property";
      tutorial.message.description = "Again, you will see more detailed information about the selected property on the right.";
    } else if (tutorial.card == 20) {
      tutorial.circle.style = {right: "calc(5% - 30px)", top: "60px", width: "200px", height: "200px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Add the property";
      tutorial.message.description = "Let's add it by clicking 'Add to filter' in the top right.";
    } else if (tutorial.card == 21) {
      this.filterRef.current.addToFilter(this.filterRef.current.state.selected);
      tutorial.circle.style = {left: "calc(5% + 450px)", top: "250px", width: "700px", height: "700px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "This is your filter";
      tutorial.message.description = "After clicking 'Add to filter' the property you have selected appears in the bottom of the window.";
    } else if (tutorial.card == 22) {    
      tutorial.circle.style = {right: "calc(5% - 10px)", top: "20px", width: "150px", height: "150px"};
      tutorial.message.style = {left: "calc(40% - 400px)"};
      tutorial.message.title = "Continue building your filter";
      tutorial.message.description = "If you are satisfied with the properties you have added, you can continue by clicking 'Next'. You can also skip this step by clicking 'Next'.";
    } else if (tutorial.card == 23) {
      this.filterRef.current.changePage(1);
      tutorial.circle.style = {left: "-100px", top: "-50px", width: "700px", height: "700px"};
      tutorial.message.style = {};
      tutorial.message.title = "Select the genotypes";
      tutorial.message.description = "You are on page 3. Here you can select genotypes for you patients.";
    } else if (tutorial.card == 24) {
      tutorial.circle.style = {right: "calc(5% - 10px)", top: "20px", width: "150px", height: "150px"};
      tutorial.message.style = {};
      tutorial.message.title = "Continue building your filter";
      tutorial.message.description = "If you are satisfied with the genotypes you have selected, continue by clicking 'Next'.";
    } else if (tutorial.card == 25) {
      this.filterRef.current.changePage(1);
      tutorial.circle.style = {left: "-100px", top: "-50px", width: "700px", height: "700px"};
      tutorial.message.style = {};
      tutorial.message.title = "Sort the data";
      tutorial.message.description = "You can also sort the variants by different fields on page 4.";
    } else if (tutorial.card == 26) {
      tutorial.circle.style = {right: "calc(5% - 10px)", top: "-5px", width: "200px", height: "200px"};
      tutorial.message.style = {};
      tutorial.message.title = "Apply the filter";
      tutorial.message.description = "You can now apply the filter to your data. In this example - after clicking 'Apply filter!' - you will only see variants on genes associated with familial melanoma and high and moderate impact that occure in less than 1% of the population.";
    } else if (tutorial.card == 27) {
      tutorial.circle.style = {display: "none"};
      tutorial.message.style = {outline: "10000px solid #000000aa"};
      tutorial.message.title = "Done!";
      tutorial.message.description = "You have sucessfully completed the tutorial! By clicking 'Continue' you will be redirected to your unfilterd variants.";
    } else if (tutorial.card == 28) {
      tutorial = null;
      this.filterRef.current.changePage(-3);
      this.filterRef.current.convertTextToFilter("Variant_quality IN [ PASS ]");
      this.filterRef.current.generateTextview();
      this.closeFilter();
    }

    this.setState(prevState => ({variants: prevState.variants, queryData: prevState.queryData, meta: prevState.meta, tutorial: tutorial, selected: prevState.selected, cursor: prevState.cursor, error: prevState.error, loading: prevState.loading, popupFilterVisibleClass: prevState.popupFilterVisibleClass}));  
  }

}

export default VariantList;
