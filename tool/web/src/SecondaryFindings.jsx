import React, { useState, useEffect } from 'react';
import './SecondaryFindings.css';

import Config from './config.js';
import Tiers from './tiers.json';

export default function Tiering(props) {

    const [patientIsChild, setPatientIsChild] = useState(false);
    const [patientHasSingleXChromosome, setPatientHasSingleXChromosome] = useState(true);

    const [onResultPage, setOnResultPage] = useState(false);
    const [loading, setLoading] = useState(false);
    const [results, setResults] = useState([]);
    const [groupedFindings, setGroupedFindings] = useState([]);

    const handleSubmission = (item) => {
        setLoading(true);
        const load = async () => {
            const result = await fetchVariants(props.token.tokenString, props.matchProps.name, patientIsChild, patientHasSingleXChromosome);
            setLoading(false);
            setResults(result);
            setGroupedFindings(groupVariantsByVid(result));
            setOnResultPage(true);
        };
        load();
    };

    if (loading) {
        return(
            <div className="Tiering">
                <div className="tieringBackground">
                    <div className="tieringBox">
                    <br/><br/>
                        <LargeLoader/><br/>
                        <div className="info">
                          <center>This may take up to a couple of minutes depending on the size of your sample.</center>
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
                            <i class="bi bi-info-circle-fill"></i> Perform a secondary findings analysis based on <a href="https://www.ncbi.nlm.nih.gov/clinvar/docs/acmg/" target="_blank">ACMG Recommendations for Reporting of Secondary Findings in Clinical Exome and Genome Sequencing</a> for the sample {props.matchProps.name}. Enter the requested information below to start!
                        </div>
                        <div className="trioBox">
                          Patient age<br/>
                          <div><input checked={patientIsChild ? "checked": ""} onChange={(e) => setPatientIsChild(true)} style={{verticalAlign: "middle"}} name="child" type="radio" value={"child"} /><label for={"child"}>Child</label></div>
                          <div><input checked={!patientIsChild ? "checked": ""} onChange={(e) => setPatientIsChild(false)} style={{verticalAlign: "middle"}} name="adult" type="radio" value={"adult"} /><label for={"adult"}>Adult</label></div>
                          <br/>
                          Patient gender/susceptibility to X-linked recessive inheritance<br/>
                          <div><input checked={!patientHasSingleXChromosome ? "checked": ""} onChange={(e) => setPatientHasSingleXChromosome(false)} style={{verticalAlign: "middle"}} name="female" type="radio" value={"female"} /><label for={"female"}>Female (or multiple X chromosomes)</label></div>
                          <div><input checked={patientHasSingleXChromosome ? "checked": ""} onChange={(e) => setPatientHasSingleXChromosome(true)} style={{verticalAlign: "middle"}} name="male" type="radio" value={"male"} /><label for={"male"}>Male (or single X chromosome)</label></div>
                        </div>
                        <div className="tieringRight"><button onClick={(e) => handleSubmission()} className="sec">Display Secondary Findings <i className="bi bi-play-circle"></i></button></div>
                    </div>
                </div>
                <br/><br/>
            </div>
        )
    } else {
        return (
            <div className="SecondaryFindings">
                <div className="tieringBackground">
                    <div className="tieringBox">
                    <button className="tert" onClick={(e) => setOnResultPage(false)}><i className="bi bi-arrow-left"> Back</i></button><br/><br/>
                        <h1>{props.matchProps.name}</h1>
                        <b>{groupedFindings.length}</b> secondary {groupedFindings.length == 1 ? "finding" : "findings"} for {props.matchProps.name} in <b>{Number(results.elapsedMilliseconds / 1000).toFixed(2)} seconds</b><br/><br/>
                        {groupedFindings.length == 0 ? "No variants found. This does not mean that the patient is not affected by genetic disorders nor does it provide any information on the carrier status." : ""}
                        {groupedFindings.map(item => {
                          return (
                             <div className="finding">
                              <div className="findingHeader">
                                <div className="findingLeft">
                                  <div className="findingGene">{item.variant?.info?.info_csq_symbol}</div>
                                </div>
                                <div className="findingRight">
                                  {item.clinvarPositive ? <div className="clinvarResult">ClinVar Known</div> : ""}
                                  <div className={"acmgResult " + item.acmgClassificationResult.acmgClassification.toLowerCase()}>ACMG {item.acmgClassificationResult.acmgClassification.replaceAll("_", " ")}</div>
                                </div>
                              </div>
                              <b>{item.secondaryFindingDefinition.phenotype}</b> - {item.secondaryFindingDefinition.domain}<br/>
                              <hr/>
                              <a target="_blank" href={Config.appBaseUrl + "/view/" + props.matchProps.name + "/" + item.variant.pid}>{item.variant?.chrom}-{item.variant?.pos}-{item.variant?.ref}-{item.variant?.alt}</a><i>(Click to view details)</i><br/>
                              <i class="bi bi-info-circle"></i> Inheritance: {item.secondaryFindingDefinition.inheritance}. Typical onset: {item.secondaryFindingDefinition.typicalOnsetChild ? "Children" : ""}{item.secondaryFindingDefinition.typicalOnsetAdult && item.secondaryFindingDefinition.typicalOnsetChild ? ", " : ""} {item.secondaryFindingDefinition.typicalOnsetAdult ? "Adults" : ""}, {item.secondaryFindingDefinition.omimId.map(omimId => {return (<a target={"_blank"} href={"https://www.omim.org/entry/" + omimId}>OMIM {omimId}</a>)})}
                             </div>
                          )
                        })}
                        <br/><br/>
                        <h3>Analysis summary for {props.matchProps.name}</h3>
                        <table className="secondaryFindings">
                          {Object.keys(results.secondaryFindingGeneSummaryDTOs).sort().map(key => {
                            return (
                              <tr className={results.secondaryFindingGeneSummaryDTOs[key].numberOfFindings > 0 ? "highlighted" : ""}>
                                <td className="padded"><b>{results.secondaryFindingGeneSummaryDTOs[key].gene}</b></td>
                                <td className="padded">{results.secondaryFindingGeneSummaryDTOs[key].numberOfVariants} variants detected</td>
                                <td className={"padded " + (results.secondaryFindingGeneSummaryDTOs[key].numberOfFindings > 0 ? "bold" : "")}>{results.secondaryFindingGeneSummaryDTOs[key].numberOfFindings} {results.secondaryFindingGeneSummaryDTOs[key].numberOfFindings == 1 ? "finding" : "findings"} across isoforms</td>
                              </tr>
                            )
                          })}
                        </table>
                        <br/>
                    </div>
                </div>
                <br/><br/><br/><br/>
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

function fetchVariants(token, sample, patientIsChild, patientHasSingleXChromosome) {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(
            {
                token: token,
                sample: sample,
                isChild: patientIsChild,
                hasSingleXChromosome: patientHasSingleXChromosome
            }
        )
    };

    return fetch(Config.apiBaseUrl + '/secondaryfindings/load', requestOptions)
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

  var canonicalList = [];
  var keyset = Object.keys(groupedVariants);
  for (var i = 0; i < keyset.length; i++) {
    var canonicalFound = false;
    for (var j = 0; j < groupedVariants[keyset[i]].length; j++) {
      var v = groupedVariants[keyset[i]][j];
      if (v?.variant?.info?.info_csq_canonical == "YES") {
        canonicalFound = true;
        canonicalList.push(v);
      }
    }
    if (!canonicalFound) {
      canonicalList.push(groupedVariants[keyset[i]][0]);
    }
  }

  console.log(canonicalList[0].secondaryFindingDefinition.gene);

  var sortedVariants = canonicalList.sort((v1, v2) => {return v1?.secondaryFindingDefinition.gene.localeCompare(v2?.secondaryFindingDefinition.gene)});
  return sortedVariants;
}