import './PanelSelector.css';
import React from 'react';  

class PanelSelector extends React.Component {

  constructor() {
    super();
    this.state = { selected: null, foundEntries: [] };
    this.filterResultsRef = React.createRef();
    this.filterDetailRef = React.createRef();
    this.searchFieldRef = React.createRef();
  }

  componentDidMount() {
    this.setState(prevState => ({selected: null, foundEntries: [], selectedIndex: 0}));
    console.log(this.props.panelIndex);
  }

  renderAllPanels() {
    if (this.props.panelIndex == null || this.props.panelIndex.panelIndex == null) {
      return;
    }

    let result = [];
    for (var i = 0; i < this.props.panelIndex.panelIndex.length; i++) {
      let currentPanel = this.props.panelIndex.panelIndex[i];
      result.push({name: currentPanel.name, type: "panel", object: currentPanel});
    }
    this.setState(prevState => ({selected: null, foundEntries: result, selectedIndex: 0}));
  }

  /**
   * called whenever a user types a query in the search bar
   */
  search(e) {
    if (this.props.panelIndex == null) {
      return;
    }

    let query = e.target.value.toLowerCase();
    //alert(query);
    let result = [];

    if (this.props.panelIndex.panelIndex == undefined) {
      return;
    }

    //find matching panels
    for (var i = 0; i < this.props.panelIndex.panelIndex.length; i++) {
      let currentPanel = this.props.panelIndex.panelIndex[i];

      let resultMatches = currentPanel.name.toLowerCase().includes(query) || currentPanel.diseaseGroup.toLowerCase().includes(query)  || currentPanel.diseaseSubGroup.toLowerCase().includes(query);

      //relevant_disorders
      if (currentPanel.relevantDisorders != null) {
        for (var j = 0; j < currentPanel.relevantDisorders.length; j++) {
          resultMatches = resultMatches || currentPanel.relevantDisorders[j].toLowerCase().includes(query);
        }
      }

      if (resultMatches) {
        result.push({name: currentPanel.name, type: "panel", object: currentPanel});
      }
    }

    if (query.length > 2 || result.length < 10) {
      //find matching genes
      for (var i = 0; i < this.props.panelIndex.geneIndex.length; i++) {
        let currentGene = this.props.panelIndex.geneIndex[i];

        let resultMatches = currentGene.name.toLowerCase().includes(query) || (currentGene.hgncId != null && currentGene.hgncId.toLowerCase().includes(query))  || currentGene.symbol.toLowerCase().includes(query);

        if (currentGene.omim != null) {
          for (var j = 0; j < currentGene.omim.length; j++) {
            resultMatches = resultMatches || currentGene.omim[j] == query;
          }
        }

        if (currentGene.aliasName != null) {
          for (var j = 0; j < currentGene.aliasName.length; j++) {
            resultMatches = resultMatches || currentGene.aliasName[j].toLowerCase().includes(query);
          }
        }

        if (currentGene.phenotypes != null) {
          for (var j = 0; j < currentGene.phenotypes.length; j++) {
            resultMatches = resultMatches || currentGene.phenotypes[j].toLowerCase().includes(query);
          }
        }

        if (resultMatches) {
          result.push({name: currentGene.symbol, type: "gene", object: currentGene});
        }
      }  

      //find aliases
      for (var i = 0; i < this.props.panelIndex.aliasIndex.length; i++) {
        let currentAlias = this.props.panelIndex.aliasIndex[i];
        if (currentAlias.alias.toLowerCase().includes(query)) {
          result.push({name: currentAlias.alias, type: "alias", object: currentAlias.gene})
        }
      }
    }

    let selected = null;

    if (result.length > 0) {
      selected = result[0];
      this.foundNothing = false;
    } else {
      this.foundNothing = true;
    }

    this.setState(prevState => ({selected: selected, foundEntries: result, selectedIndex: 0}));

    if (this.filterResultsRef.current != null) {
      this.filterResultsRef.current.scrollTop = 0;
    }
  }  

  select(index) {
    this.setState(prevState => ({selected: this.state.foundEntries[index], foundEntries: prevState.foundEntries, selectedIndex: index}));
  }  

  panelNameToHumanReadableId(name) {
    return name.replaceAll(/[^a-zA-Z0-9\s]/g, "").replaceAll("  ", " ").replaceAll(" ", "_").toUpperCase();
  }

  selectGeneBySymbol(symbol) {
    let selected = null;
    for (var i = 0; i < this.props.panelIndex.geneIndex.length; i++) {
      if (this.props.panelIndex.geneIndex[i].symbol.toLowerCase() == symbol.toLowerCase()) {
        selected = {name: symbol, type: "gene", object: this.props.panelIndex.geneIndex[i]};
        break;
      }
    }

    this.setState(prevState => ({selected: selected, foundEntries: prevState.foundEntries, selectedIndex: prevState.selectedIndex}));

    if (this.filterDetailRef.current != null) {
      this.filterDetailRef.current.scrollTop = 0;
    }
  }

  selectPanelByName(name) {
    let selected = null;
    for (var i = 0; i < this.props.panelIndex.panelIndex.length; i++) {
      if (this.props.panelIndex.panelIndex[i].name == name) {
        selected = {name: name, type: "panel", object: this.props.panelIndex.panelIndex[i]};
        break;
      }
    }

    this.setState(prevState => ({selected: selected, foundEntries: prevState.foundEntries, selectedIndex: prevState.selectedIndex}));

    if (this.filterDetailRef.current != null) {
      this.filterDetailRef.current.scrollTop = 0;
    }
  }  

  render() {
    if (this.searchFieldRef.current != null && this.searchFieldRef.current.value.length == 0 && this.props.panelIndex != null && this.state.foundEntries.length <= 0) {
      this.renderAllPanels();
    }

    let filterDetail = <div className="filterDetail filterDetailSmall">
        In this step you can apply gene panels or search for specific or sets of genes. The gene panels available to you are curated by Genomics England and available via their <a href="https://panelapp.genomicsengland.co.uk" target="_blank">Panel App</a>. Genes with a green confidence level provide diagnostic levels of evidence. These genes are used in genome interpretation by the National Health Service (NHS) England for genomic interpretation. The confidence level for a gene might vary for different diseases. These panels are reviewed and updated by an annual and quarterly evaluation process supported by expert test evaluation working groups and overseen by the NHS England Genomics Clinical Reference Group. This tool incorporates updates automatically on a daily basis.
      </div>

    if (this.state.foundEntries == null || (this.state.foundEntries != null && this.state.foundEntries.length <= 0)) {
      filterDetail = <div className="filterDetail filterDetailSmall"><div class="spinner-border spinner-border" role="status"></div><br/>Loading panels...</div>
    }

    if (this.foundNothing) {
      filterDetail = <div className="filterDetail filterDetailSmall">
                      No panels, dieseases or genes found
                    </div>;
    } else if (this.state.selected != null) {
      if (this.state.selected.type == "alias") {
        //find gene with right symbol
        this.selectGeneBySymbol(this.state.selected.object);
      }

      if (this.state.selected.type == "panel") {
        filterDetail = <div ref={this.filterDetailRef} className="filterDetail filterDetailSmall">
                          <div className="floatright"><button onClick={(e) => this.props.addPanelToFilter(this.panelNameToHumanReadableId(this.state.selected.object.name))} className="sec">Add panel to filter</button></div>
                          <div className="head flex-container">
                            <div className="title titleWide">
                              {this.state.selected.object.name}
                            </div>
                          </div>
                          <div className="body">
                            <div className="diseaseGroup"><span className="mainDiseaseGroup">{this.state.selected.object.diseaseGroup}</span> {this.state.selected.object.diseaseSubGroup}</div>
                            {
                              (this.state.selected.object.relevantDisorders != null && this.state.selected.object.relevantDisorders.length > 0) ? 
                              <div className="relevantDisorders">
                                <b>Relevant disorders: </b>
                                <ul>
                                {this.state.selected.object.relevantDisorders.map((item, index) => {
                                  return <li className="relevantDisorder">{item}</li>;
                                })}
                                </ul>
                              </div> : ""
                            }
                            <div className="geneSymbols">
                              <b>Genes in panel: </b>
                              {this.state.selected.object.geneSymbols != null ? this.state.selected.object.geneSymbols.map((item, index) => {
                                return <span onClick={(e) => this.selectGeneBySymbol(item)} className="geneSymbol">{item}</span>;
                              }) : ""}
                            </div>
                          </div>
                        </div>;
      } else if (this.state.selected.type == "gene") {
        filterDetail = <div ref={this.filterDetailRef} className="filterDetail filterDetailSmall"> 
                          <div className="floatright"><button onClick={(e) => this.props.addGeneToFilter(this.state.selected.object.symbol)} className="sec">Add gene to filter</button></div>
                          <div className="head flex-container">
                            <div className="title titleWide">
                              {this.state.selected.object.symbol}
                            </div>
                          </div>
                          <div className="body">
                            <div className="fullGeneName">{this.state.selected.object.name}</div>
                            <div><a target="_blank" href={"https://www.genenames.org/data/gene-symbol-report/#!/hgnc_id/" + this.state.selected.object.hgncId}>{this.state.selected.object.hgncId}</a></div>
                            {this.state.selected.object.omim != null ? this.state.selected.object.omim.map((item, index) => {
                              return <div><a target="_blank" href={"https://www.omim.org/entry/" + item}>{this.state.selected.object.symbol} on Omim</a></div>;
                            }) : ""}
                            <br/>               

                            <div className="panelNames">
                              <b>Panels including this gene: </b>
                              {this.state.selected.object.panelNames != null ? this.state.selected.object.panelNames.map((item, index) => {
                                return <span onClick={(e) => this.selectPanelByName(item)} className="panelName">{item}</span>;
                              }) : ""}
                            </div>
                          </div>
                        </div>;
      }
    }

    return <div>
        <div className="filterSelector flex-container">
          <div className="filterList filterListLarge">
            <div className="head">
              <div className="inner-addon left-addon">
                  <i className="glyphicon bi bi-search"></i>
                  <input ref={this.searchFieldRef} placeholder="search for a panel, gene or disease" onChange={(e) => this.search(e)} type="text" class="form-control" />
              </div>
            </div>
            <div ref={this.filterResultsRef} className="body">
              {this.state.foundEntries.map((item, index) => {
                if (item.type.toLowerCase() == "panel") {
                  return <div onClick={(e) => this.select(index)} className={"filterRow " + (this.state.selectedIndex == index ? "selected" : "")}>{item.name} <span onClick={(e) => {this.select(index); this.props.addPanelToFilter(this.panelNameToHumanReadableId(item.object.name))}} className={"filterRowType type" + item.type}>Add this {item.type}</span></div>
                } else {
                  let geneName = item.object; //for aliases
                  if (item.type.toLowerCase() == "gene") {
                    geneName = item.object.symbol;
                  }
                  return <div onClick={(e) => this.select(index)} className={"filterRow " + (this.state.selectedIndex == index ? "selected" : "")}>{item.name} <span onClick={(e) => {this.select(index); this.props.addGeneToFilter(geneName);}} className={"filterRowType type" + item.type}>Add this {item.type}</span></div>
                }
              })}
            </div>
          </div>
          {filterDetail}
        </div>
      </div>;
  } 

}

export default PanelSelector;
