import React, { useState, useEffect } from 'react';
import './Tiering.css';

import Config from './config.js';

export default function Tiering(props) {

    const [searchData, setSearchData] = useState({panels: null, hpoTerms: null});
    const [searchDataLoaded, setSearchDataLoaded] = useState(false);

    const [query, setQuery] = useState("");
    const [searchResults, setSearchResults] = useState([]);

    const [selected, setSelected] = useState([]);

    const [onResultPage, setOnResultPage] = useState(false);
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);

    const handleQueryChange = event => {
        setQuery(event.target.value);
    };

    const handleSelectResult = (item) => {
        if (!selected.includes(item)) {
            setSelected(selected.concat([item]))
        }
    };

    const handleTiering = (item) => {
        setLoading(true);
        const load = async () => {
            const variants = await fetchVariants(props.token.tokenString, props.matchProps.name, selected);
            setLoading(false);
            setResults(variants);
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

        for (var i = 0; i < searchData.hpoTerms.hpoTerms.length; i++) {
            var hpoTerm = searchData.hpoTerms.hpoTerms[i];
            if (hpoTerm.name.toLowerCase().startsWith(queryClean)) {
                if (!found.has(hpoTerm)) {
                    resultStartsWith.push({type: 'hpoTerm', value: hpoTerm});
                    found.add(hpoTerm);
                }
            }
        }

        for (var i = 0; i < searchData.hpoTerms.hpoTerms.length; i++) {
            var hpoTerm = searchData.hpoTerms.hpoTerms[i];
            if (hpoTerm.name.toLowerCase().includes(queryClean)) {
                if (!found.has(hpoTerm)) {
                    resultIncludes.push({type: 'hpoTerm', value: hpoTerm});
                    found.add(hpoTerm);
                }
            }
        }

        for (var i = 0; i < searchData.hpoTerms.hpoTerms.length; i++) {
            var hpoTerm = searchData.hpoTerms.hpoTerms[i];
            for (var j = 0; j < hpoTerm.synonyms.length; j++) {
                if (hpoTerm.synonyms[j].includes(queryClean)) {
                    if (!found.has(hpoTerm)) {
                        resultSynonym.push({type: 'hpoTerm', value: hpoTerm});
                        found.add(hpoTerm);
                    }
                }
            }
        }

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
          console.log(panels);
          setSearchData({panels: panels, hpoTerms: hpoTerms});
          setSearchDataLoaded(true);
        };
        load();
    }, []);

    if (loading) {
        return(
            <div className="Tiering">
                <div className="tieringBackground">
                    <div className="tieringBox">
                        <LargeLoader/>
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
                        {selected.map(item => (
                            <div className="tieringSelected">{item.value.name} <i onClick={(e) => handleRemove(item)} className="pointer bi bi-x"></i></div>
                        ))}
                        {selected.length > 1 ? <a href='#' onClick={(e) => handleRemove(null)}><i>clear</i></a> : ""}
                        {selected.length > 0 ? <div className="tieringRight"><button onClick={(e) => handleTiering()} className="sec">Perform Tiering <i className="bi bi-play-circle"></i></button></div> : ""}
                    </div>
                </div>
                <br/><br/>
                check for trios, add explaining for tiers. There might be a problem with the likely pathogenic stuff. This should be checked the same way the enum is in a filter<br/>
            </div>
        )
    } else {
        return (
            <div className="Tiering">
                <div className="tieringBackground">
                    <div className="tieringBox">
                        <button className="tert" onClick={(e) => setOnResultPage(false)}><i className="bi bi-arrow-left"> Back to search</i></button><br/><br/>
                        <h1>Possible causative variants</h1>
                        <b>{results.variants.length}</b> variants found in <b>{Number(results.elapsedMilliseconds / 1000).toFixed(2)} seconds</b><br/><br/>
                        {results.variants.map(item => (
                            <div className="tieringResult">
                                <div className="resultName">
                                    <a target="_blank" href={Config.appBaseUrl + "/view/" + props.matchProps.name + "/" + item.variant.pid}>
                                        {item.variant.info["info_csq_hgvsc"]}
                                    </a>
                                </div>
                                <div className="resultTiers">
                                    {item.acmgTiers.map(tier => (
                                        <div className={"tier " + tier.replace(/[0-9]/g, "")}>{tier}</div>
                                    ))}
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

    return fetch(Config.apiBaseUrl + '/knowledgebase/panelapp/getpanelindex', requestOptions)
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

function fetchVariants(token, sample, selected) {

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
                humansDTO: null
            }
        )
    };

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