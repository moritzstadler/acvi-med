import React, { useState, useEffect } from 'react';
import './Tiering.css';

import Config from './config.js';
import Tiers from './tiers.json';

export default function Tiering(props) {

    const [searchData, setSearchData] = useState({panels: null, hpoTerms: null, meta: null});
    const [searchDataLoaded, setSearchDataLoaded] = useState(false);

    const [query, setQuery] = useState("");
    const [searchResults, setSearchResults] = useState([]);

    const [indexPerson, setIndexPerson] = useState(null);
    const [affectedPatients, setAffectedPatients] = useState([]);
    const [parenthoodConfirmed, setParenthoodConfirmed] = useState(false);
    const [resultVisibility, setResultVisibility] = useState([]);

    const [selected, setSelected] = useState([]);

    const [onResultPage, setOnResultPage] = useState(false);
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);

    const [openExplanations, setOpenExplanations] = useState([]);

    const handleQueryChange = event => {
        setQuery(event.target.value);
    };

    const toggleAffectedPatient = (item) => {
      var newAffectedPatients = affectedPatients.slice();
      if (affectedPatients.includes(item)) {
        setAffectedPatients(newAffectedPatients.filter(p => p !== item));
      } else {
        newAffectedPatients.push(item);
        setAffectedPatients(newAffectedPatients);
      }
    };

    const toggleTierView = (id) => {
      if (openExplanations.includes(id)) {
        setOpenExplanations(openExplanations.filter(oe => oe != id));
      } else {
        setOpenExplanations([id]);
      }
      console.log(openExplanations);
    };

    const getTierExplanation = (tier) => {
      return Tiers[tier];
    }

    const handleSelectResult = (item) => {
      if (!selected.includes(item)) {
          setSelected(selected.concat([item]))
      }
    };

    const toggleResultVisibility = (index) => {
      var newResultVisibility = resultVisibility.slice();
      if (newResultVisibility[index] === undefined) {
        newResultVisibility[index] = false;
      }
      newResultVisibility[index] = !newResultVisibility[index];
      setResultVisibility(newResultVisibility);
      console.log(resultVisibility);
    }

    const handleTiering = (item) => {
        setLoading(true);
        const load = async () => {
            var humans = [];
            var formats = getFormats(searchData.meta);
            for (var i = 0; i < formats.length; i++) {
              var format = formats[i];
              humans.push({pseudonym: format, isIndex: indexPerson === format, isAffected: affectedPatients.includes(format)});
            }

            const variants = await fetchVariants(props.token.tokenString, props.matchProps.name, selected, humans, parenthoodConfirmed);
            setLoading(false);
            const variantsGroupedByVid = groupVariantsByVid(variants);
            setResults(variantsGroupedByVid);
            setOnResultPage(true);
        };
        load();
    };

    const handleRemove = (item) => {
        if (item == null) {
            setSelected([]);
            return;
        }

        var s = [];

        for (var i = 0; i < selected.length; i++) {
            if (selected[i] !== item) {
                s.push(selected[i]);
            }
        }

        setSelected(s);
    };

    useEffect(() => {
        if (query === "") {
            setSearchResults([]);
            return;
        }
        const queryClean = query.toLowerCase();

        var resultStartsWith = [];
        var resultIncludes = [];
        var resultSynonym = [];

        var found = new Set();

        for (var i = 0; i < searchData.panels.panelIndex.length; i++) {
            var panel = searchData.panels.panelIndex[i];
            if (panel.name.toLowerCase().startsWith(queryClean)) {
                if (!found.has(panel)) {
                    resultStartsWith.push({type: 'panel', value: panel});
                    found.add(panel);
                }
            }
        }

        for (var i = 0; i < searchData.panels.panelIndex.length; i++) {
            var panel = searchData.panels.panelIndex[i];
            if (panel.name.toLowerCase().includes(queryClean)) {
                if (!found.has(panel)) {
                    resultIncludes.push({type: 'panel', value: panel});
                    found.add(panel);
                }
            }
        }

        for (var i = 0; i < searchData.panels.panelIndex.length; i++) {
            var panel = searchData.panels.panelIndex[i];
            var disorderIncluded = false;
            for (var j = 0; j < panel.relevantDisorders.length; j++) {
                if (panel.relevantDisorders[j].toLowerCase().includes(queryClean)) {
                    disorderIncluded = true;
                    break;
                }
            }
            if (disorderIncluded || panel.name.toLowerCase().includes(queryClean) || panel.diseaseGroup.toLowerCase().includes(queryClean) || panel.diseaseSubGroup.toLowerCase().includes(queryClean)) {
                if (!found.has(panel)) {
                    resultSynonym.push({type: 'panel', value: panel});
                    found.add(panel);
                }
            }
        }

        for (var i = 0; i < searchData.hpoTerms.hpoTerms.length; i++) {
            var hpoTerm = searchData.hpoTerms.hpoTerms[i];
            if (hpoTerm?.name?.toLowerCase().startsWith(queryClean)) {
                if (!found.has(hpoTerm)) {
                    resultStartsWith.push({type: 'hpoTerm', value: hpoTerm});
                    found.add(hpoTerm);
                }
            }
        }

        for (var i = 0; i < searchData.hpoTerms.hpoTerms.length; i++) {
            var hpoTerm = searchData.hpoTerms.hpoTerms[i];
            if (hpoTerm?.name?.toLowerCase().includes(queryClean)) {
                if (!found.has(hpoTerm)) {
                    resultIncludes.push({type: 'hpoTerm', value: hpoTerm});
                    found.add(hpoTerm);
                }
            }
        }

        for (var i = 0; i < searchData.hpoTerms.hpoTerms.length; i++) {
            var hpoTerm = searchData.hpoTerms.hpoTerms[i];
            for (var j = 0; j < hpoTerm?.synonyms?.length; j++) {
                if (hpoTerm?.synonyms[j]?.includes(queryClean)) {
                    if (!found.has(hpoTerm)) {
                        resultSynonym.push({type: 'hpoTerm', value: hpoTerm});
                        found.add(hpoTerm);
                    }
                }
            }
        }

        var result = resultStartsWith.concat(resultIncludes, resultSynonym);
        var trimmedResult = [];

        for (var i = 0; i < Math.min(300, result.length); i++) {
            trimmedResult.push(result[i]);
        }

        setSearchResults(trimmedResult);
    }, [query]);

    useEffect(() => {
        const load = async () => {
          const panels = await fetchPanels(props);
          const hpoTerms = await fetchHpoTerms(props);
          const meta = await fetchMeta(props.token.tokenString, props.matchProps.name);
          console.log(panels);
          console.log(meta);
          setSearchData({panels: panels, hpoTerms: hpoTerms, meta: meta});
          setSearchDataLoaded(true);
        };
        load();
    }, []);

    if (loading) {
        return(
            <div className="Tiering">
                <div className="tieringBackground">
                    <div className="tieringBox">
                        <LargeLoader/><br/><br/>
                        <div className="info">
                          This may take up to a couple of minutes depending on the size of your sample and the complexity of your query.<br/>
                          <ul>
                            <li>The variants are selected by gene or HPO-Term according to your query</li>
                            <li>Each resulting variant is tiered according to the <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4544753/" target="_blank">ACMG-Guidelines</a></li>
                            <li>Tiered variants are sorted by pathogenicity - highest to lowest</li>
                          </ul>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    if (!onResultPage) {
        return (
            <div className="Tiering">
                <div className="tieringBackground">
                    <div className="tieringBox">
                        <h1>{props.matchProps.name}</h1>
                        <div className="info">
                            Perform an <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4544753/" target="_blank">ACMG-Tiering</a> for the sample {props.matchProps.name}. Enter the observed phenotypes below to start!
                        </div>
                        {searchDataLoaded && getFormats(searchData.meta).length > 1 ?
                          <div className="trioBox">
                            <b><i class="bi bi-info-circle-fill"></i> You are working with a trio! Improve the results by defining the index person and affected family members.</b><br/>
                            Select the index person (primary affected patient, child):
                            {getFormats(searchData.meta).map(item => (
                              <div><input checked={indexPerson == item ? "checked": ""} onChange={(e) => setIndexPerson(e.currentTarget.value)} style={{verticalAlign: "middle"}} name="indexPerson" type="radio" value={item} /><label for={item}>{item}</label></div>
                            ))}
                            <br/>
                            Define who is affected by the disease:
                            {getFormats(searchData.meta).map(item => (
                              <div><input checked={affectedPatients.includes(item) ? "checked": ""} onChange={(e) => {toggleAffectedPatient(e.target.value)}} value={item} style={{verticalAlign: "middle"}} type="checkbox" /><label>{item}</label></div>
                            ))}
                            <br/>
                            Was both the maternity and paternity confirmed: <input onChange={(e) => {setParenthoodConfirmed(e.target.value)}} value={parenthoodConfirmed} type="checkbox" />
                          </div>
                          : null
                        }
                        {searchDataLoaded && getFormats(searchData.meta).length > 1 && selected.map(item => (
                            <div className="tieringSelected">{item.value.name} <i onClick={(e) => handleRemove(item)} className="pointer bi bi-x"></i></div>
                        ))}
                        {searchDataLoaded ?
                            <input value={query} onChange={handleQueryChange} placeholder="Phenotype" className="tieringInput" />
                            : <Loader/>
                        }
                        <div className="searchResults">
                            {searchResults.map(item => (
                                <div onClick={(e) => handleSelectResult(item)} className="searchResult"><div className={item.type + "Badge"}>{item.type == "hpoTerm" ? "HPO-Term" : "Panel"}</div> <b>{item.value.name}</b></div>
                            ))}
                            {searchResults.length == 0 && query.length !== 0 ? <span className="info">no results found</span> : ""}
                        </div>
                        {searchDataLoaded && getFormats(searchData.meta).length <= 1 && selected.map(item => (
                            <div className="tieringSelected">{item.value.name} <i onClick={(e) => handleRemove(item)} className="pointer bi bi-x"></i></div>
                        ))}
                        {selected.length > 1 ? <a href='#' onClick={(e) => handleRemove(null)}><i>clear</i></a> : ""}
                        {selected.length > 0 ? <div className="tieringRight"><button onClick={(e) => handleTiering()} className="sec">Perform Tiering <i className="bi bi-play-circle"></i></button></div> : ""}
                    </div>
                </div>
                <br/><br/>
            </div>
        )
    } else {
        return (
            <div className="Tiering">
                <div className="tieringBackground">
                    <div className="tieringBox">
                        <h1>{props.matchProps.name}</h1>
                        <button className="tert" onClick={(e) => setOnResultPage(false)}><i className="bi bi-arrow-left"> Back to search</i></button><br/><br/>
                        <h1>Possible causative variants</h1>
                        <b>{results.variants.length}</b> variants found in <b>{Number(results.elapsedMilliseconds / 1000).toFixed(2)} seconds</b><br/><br/>
                        <div className="info"><i class="bi bi-info-circle-fill"></i> Click on the name of a tier in order to view its definition.</div>
                        {results.variants.map((item, i) => (
                            <div className="tieringResult">
                                <div onClick={(e) => toggleResultVisibility(i)} className="resultName">
                                  <i className={"glyphicon bi bi-caret-" + (resultVisibility[i] === true ? "down" : "right") + "-fill"} ></i> <b>{item.chrom}:{item.pos}</b> <span className="note">click to view isoforms and details</span>
                                </div>
                                <div className={"resultTiers " + (resultVisibility[i] === true ? "hidden" : "")}>
                                    {item.acmgTiers.map(tier => (
                                        <div onClick={(e) => {toggleTierView(i + ".0." + tier); toggleResultVisibility(i)}} className="tierBox"><div className={"tier " + tier.replace(/[0-9]/g, "")}>{tier}</div></div>
                                    ))}
                                    <div onClick={(e) => {toggleTierView(i + ".0.classification"); toggleResultVisibility(i)}} className="classificationBox"><div className={"classification " + item.acmgClassification?.toLowerCase()}>{item.acmgClassification?.replaceAll("_", " ")}</div></div>
                                </div>
                                <div  className={"isoforms"}>
                                  {resultVisibility[i] ?
                                    <div>
                                      {item.isoforms.map((isoform, index) => (
                                        <div className="isoform">
                                          <div className="resultName">

                                              <a target="_blank" href={Config.appBaseUrl + "/view/" + props.matchProps.name + "/" + isoform.variant.pid}>
                                                {isoform.variant.info["info_csq_hgvsc"] !== null ? isoform.variant.info["info_csq_hgvsc"] : isoform.variant.info["info_csq_hgvsg"]}
                                                {isoform.variant.info["info_csq_canonical"] && <span className="smallInlineBox" style={{background: "#00a087"}}>Canonical</span>}
                                              </a>
                                          </div>
                                          <div className="resultTiers">
                                            {isoform.acmgTieringResults.map(tr => (
                                                <div onClick={(e) => toggleTierView(i + "." + index + "." + tr.tier)} className="tierBox"><div className={"tier " + tr.tier.replace(/[0-9]/g, "")}>{tr.tier}</div></div>
                                            ))}
                                            <div onClick={(e) => toggleTierView(i + "." + index + ".classification")} className="classificationBox"><div className={"classification " + isoform.acmgClassificationResult.acmgClassification?.toLowerCase()}>{isoform.acmgClassificationResult.acmgClassification?.replaceAll("_", " ")}</div></div>
                                          </div>
                                          {openExplanations.includes(i + "." + index + ".classification") ?
                                            <div className={"tierExplanation"}>
                                              <div className="tierTitle">ACMG classification: {isoform.acmgClassificationResult.acmgClassification?.replaceAll("_", " ")}</div>
                                              <hr/>
                                              <div className="info">Causes why this variant was classified as {isoform.acmgClassificationResult.acmgClassification?.replaceAll("_", " ")}:</div>
                                              {Object.keys(isoform.acmgClassificationResult.explanation).sort((a, b) => {return a.localeCompare(b)}).map(k => (
                                                <div className="tierCheckMark"><i class="bi bi-check-lg"></i> {k}: <b>{isoform.acmgClassificationResult.explanation[k]}</b></div>
                                              ))}
                                            </div>
                                            : ""
                                          }
                                          {isoform.acmgTieringResults.map(tr => {
                                              return (
                                                <div>
                                                  {openExplanations.includes(i + "." + index + "." + tr.tier) ?
                                                    <div className={"tierExplanation"}>
                                                      <div className="tierTitle">{tr.tier}</div>
                                                      <b>{getTierExplanation(tr.tier)?.title}</b><br/>
                                                      <div className="info">{getTierExplanation(tr.tier)?.description}</div>
                                                      <hr/>
                                                      <div className="info">Causes for why {tr.tier} applies to this variant:</div>
                                                      {Object.keys(tr.explanation).map(k => (
                                                        <div className="tierCheckMark"><i class="bi bi-check-lg"></i> {k}: <b>{tr.explanation[k]}</b></div>
                                                      ))}
                                                    </div>
                                                    : ""
                                                  }
                                                </div>
                                              )
                                          })}
                                        </div>
                                      ))}
                                    </div>
                                    : ""
                                  }
                                </div>
                                {results.variants.length == 0 ? <span className="info">No matching variants found. Try modifying your phenotypes!</span> : ""}
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        )
    }
}

function Loader() {
    return (
        <div className="Loader"><div className="spinner-border spinner-border-sm" role="status"></div> Setting up...</div>
    )
}

function LargeLoader() {
    return (
        <div className="LargeLoader"><div className="spinner-border spinner-border-l" role="status"></div><br/>Loading...</div>
    )
}

function fetchPanels(props) {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: props.token.tokenString })
    };

    return fetch(Config.apiBaseUrl + '/knowledgebase/panelapp/getgreenpanelindex', requestOptions)
      .then(
        response => {
          return response.json()
        }
      )
      .then(
        data => {
          return data;
        }
      );
}

function fetchHpoTerms(props) {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: props.token.tokenString })
    };

    return fetch(Config.apiBaseUrl + '/knowledgebase/hpo/gethpoterms', requestOptions)
      .then(
        response => {
          return response.json()
        }
      )
      .then(
        data => {
          return data
        }
      );
}

function fetchVariants(token, sample, selected, humans, parenthoodConfirmed) {

    var genes = [];
    var hpoTerms = [];

    for (var i = 0; i < selected.length; i++) {
        var item = selected[i];
        if (item.type === "hpoTerm") {
            hpoTerms.push(item.value.id);
        } else {
            genes = genes.concat(item.value.geneSymbols);
        }
    }

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(
            {
                token: token,
                sample: sample,
                genes: genes,
                hpoTerms: hpoTerms,
                humansDTO: {
                  humans: humans,
                  parentHoodConfirmed: parenthoodConfirmed
                }
            }
        )
    };

    console.log(requestOptions);

    return fetch(Config.apiBaseUrl + '/phenotypeaware/load', requestOptions)
      .then(
        response => {
          return response.json()
        }
      )
      .then(
        data => {
          console.log(data)
          return data
        }
      );
}

function fetchMeta(token, sample) {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: token, sample: sample })
    };

    return fetch(Config.apiBaseUrl + '/variant/loadmeta', requestOptions)
      .then(response => response.json())
      .then(
        data => {
          return data
        }
      );
}

function getFormats(meta) {
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

function groupVariantsByVid(result) {
  var variants = result.variants;
  var groupedVariants = {};

  for (var i = 0; i < variants.length; i++) {
    var variant = variants[i].variant;
    if (!(variant.vid in groupedVariants)) {
      groupedVariants[variant.vid] = [];
    }
    groupedVariants[variant.vid].push(variants[i]);
  }

  console.log(groupedVariants);

  var resultingVariants = [];
  var keys = Object.keys(groupedVariants);

  console.log(keys);

  for (var i = 0; i < keys.length; i++) {
    var acmgTiers = [];
    for (var j = 0; j < groupedVariants[keys[i]].length; j++) {
      for (var k = 0; k < groupedVariants[keys[i]][j].acmgTieringResults.length; k++) {
        if (!(acmgTiers.includes(groupedVariants[keys[i]][j].acmgTieringResults[k].tier))) {
          acmgTiers.push(groupedVariants[keys[i]][j].acmgTieringResults[k].tier);
        }
      }
    }

    var order = ["PVS1", "PS1", "PS2", "PS3", "PS4",
        "PM1", "PM2", "PM3", "PM4", "PM5", "PM6",
        "PP1", "PP2", "PP3", "PP4", "PP5", "BA1",
        "BS1", "BS2", "BS3", "BS4",
        "BP1", "BP2", "BP3", "BP4", "BP5", "BP6", "BP7"];

    var acmgTiersSorted = acmgTiers.sort((n1, n2) => order.indexOf(n1) - order.indexOf(n2));


    var orderClassification = ["PATHOGENIC", "LIKELY_PATHOGENIC", "UNCERTAIN_SIGNIFICANCE", "LIKELY_BENIGN", "BENIGN"];
    var acmgClassificationIndex = orderClassification.length - 1;
    for (var j = 0; j < groupedVariants[keys[i]].length; j++) {
      var currentVariant = groupedVariants[keys[i]][j];
      var currentClassificationIndex = orderClassification.indexOf(currentVariant.acmgClassificationResult.acmgClassification);
      if (currentClassificationIndex < acmgClassificationIndex) {
        acmgClassificationIndex = currentClassificationIndex;
      }
    }

    var group = {
      chrom: groupedVariants[keys[i]][0].variant.chrom,
      pos: groupedVariants[keys[i]][0].variant.pos,
      isoforms: groupedVariants[keys[i]],
      acmgTiers: acmgTiersSorted,
      acmgClassification: orderClassification[acmgClassificationIndex]
    };
    resultingVariants.push(group);
  }

  var resultingVariantsSorted = resultingVariants.sort((v1, v2) => {
    var classificationComparator = orderClassification.indexOf(v1.acmgClassification) - orderClassification.indexOf(v2.acmgClassification);
    if (classificationComparator == 0) {
      var a = v1.acmgTiers.map(x => order.indexOf(x));
      var b = v2.acmgTiers.map(x => order.indexOf(x));
      return Math.min(...a.filter(x => !b.includes(x)), order.length) - Math.min(...b.filter(x => !a.includes(x)), order.length);
    }
    return classificationComparator;
  });

  result.variants = resultingVariantsSorted;
  return result;
}
