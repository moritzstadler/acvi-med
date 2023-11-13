import React, { useState, useEffect } from 'react';
import './PharmacoGenomics.css';

import Config from './config.js';

export default function PharmacoGenomics(props) {

    const [onResultPage, setOnResultPage] = useState(false);
    const [loading, setLoading] = useState(false);

    const [fetchingProgress, setFetchingProgress] = useState(false);
    const [fetchingProgressCount, setFetchingProgressCount] = useState(0);
    const [processed, setProcessed] = useState(0);
    const [total, setTotal] = useState(0);
    const [time, setTime] = useState("");

    const [processId, setProcessId] = useState(null);

    const [results, setResults] = useState([]);
    const [windowData, setWindowData] = useState(null);

    const [genotypeKey, setGenotypeKey] = useState("");

    const handleSubmission = (item) => {
        setLoading(true);
        const load = async () => {
            const result = await fetchStartProcessing(props.token.tokenString, props.matchProps.name);
            setProcessId(result);
            setFetchingProgress(true);
        };
        load();
    };

    useEffect(async () => {
      if (fetchingProgress) {
        const result = await fetchProgress(props.token.tokenString, props.matchProps.name, processId);
        setProcessed(result.processed);
        setTotal(result.total);
        if (result.processed > 0) {
          if (result.elapsed > 3600000) {
            setTime(new Date(result.elapsed * (result.total / result.processed) - result.elapsed).toISOString().slice(11, 19));
          } else {
            setTime(new Date(result.elapsed * (result.total / result.processed) - result.elapsed).toISOString().slice(14, 19));
          }
        }

        if (result.processed > 0 && result.total == result.processed) {
          loadResults();
        }
      }

      const timer = setTimeout(() => fetchingProgress && setFetchingProgressCount(fetchingProgressCount+1), 500)
      return () => clearTimeout(timer)
    }, [fetchingProgress, fetchingProgressCount]);

    const loadResults = async () => {
        if (onResultPage) {
          return false;
        }

        setFetchingProgress(false);
        const result = await fetchResults(props.token.tokenString, props.matchProps.name, processId);
        setResults(result.variants);

        if (result.variants.length > 0) {
          let keys = Object.keys(result.variants[0].variant.info);
          for (const key of keys) {
            if (key.endsWith("_gt")) {
              setGenotypeKey(key);
            }
          }
        }

        setLoading(false);
        setOnResultPage(true);
    }

    if (loading) {
        return(
            <div className="Tiering">
                <div className="tieringBackground">
                    <div className="tieringBox">
                    <h1>{props.matchProps.name}</h1><br/>
                    {!(total == processed && loading) || processed == 0 ?
                      <div className="largeText">
                        {processed}/{total} relevant variants checked
                        <div className="info">{time} remaining</div><br/>
                      </div>
                      :
                      <div>
                        <LargeLoader /><br/><br/>
                      </div>
                    }
                    <div className="progressBarBackground">
                      <div className="progressBar" style={{width: (100 * processed / total) + "%"}}>
                      </div>
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
                            <i className="bi bi-info-circle-fill"></i> Perform a pharmacogenomic analysis based on <a href="https://www.pharmgkb.org/" target="_blank">PharmGKB</a> for the sample {props.matchProps.name}. Click on the button below to start!
                        </div>
                        <br/>
                        <div className="tieringRight"><button onClick={(e) => handleSubmission()} className="sec">Compute patient specific Pharmacogenomics <i className="bi bi-play-circle"></i></button></div>
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
                        <div className="info">
                            <i className="bi bi-info-circle-fill"></i> The following variants of the patient {props.matchProps.name} are associated with entries in <a href="https://www.pharmgkb.org/" target="_blank">PharmGKB</a>
                        </div>
                        {results
                          .sort((a, b) => {console.log(a.pharmGKBAnnotations.map(p => p.pharmGKBAnnotation.levelOfEvidence)); if (a.pharmGKBAnnotations.map(p => p.pharmGKBAnnotation.levelOfEvidence).sort((a, b) => a.localeCompare(b)) > b.pharmGKBAnnotations.map(p => p.pharmGKBAnnotation.levelOfEvidence).sort((a, b) => a.localeCompare(b)) ) return 1; else return -1})
                          .map(item => (
                          <div className="pharmVariant">
                            <div className="variantTitle">
                              {item.variant.chrom}:{item.variant.pos} {item.variant.ref}>{item.variant.alt}<br/>
                              <div className="info">{item.variant.info[genotypeKey]} | {item.variant.info.info_csq_symbol} {item.variant.info.info_csq_symbol == null ? item.variant.info.info_csq_hgvsc : item.variant.info.info_csq_hgvsc?.split(":")[1]} <a target="_blank" href={"/view/" + props.matchProps.name + "/" + item.variant.pid}>Click to view variant</a></div>
                              {item.pharmGKBAnnotations
                                .sort((a, b) => a.pharmGKBAnnotation.levelOfEvidence > b.pharmGKBAnnotation.levelOfEvidence ? 1 : -1)
                                .map((annotation, index) => (
                                <div onClick={(e) => setWindowData({variant: item.variant, annotation: annotation})}>
                                  <div className="annotationThumb">
                                    <div title="level of evidence (1A best, 4 worst) https://www.pharmgkb.org/page/clinAnnLevels" className={"levelOfEvidence levelOfEvidence" + annotation.pharmGKBAnnotation.levelOfEvidence} >{annotation.pharmGKBAnnotation.levelOfEvidence}</div>
                                    {annotation.pharmGKBAnnotation.drugs.split(";").map(drug => (
                                      <div className="drug">{drug.toUpperCase()}</div>
                                    ))}
                                    <br/>
                                    <div className="phenotypeCategory">{annotation.pharmGKBAnnotation.phenotypeCategory}</div>
                                    {annotation.pharmGKBAnnotation.phenotypes == "" ? "" : <div className="phenotypes">{annotation.pharmGKBAnnotation.phenotypes.split(";").map(pt => (<div className="pharmPhenotypePlain">{pt}</div>))}</div>}
                                  </div>
                                  {index < item.pharmGKBAnnotations.length - 1 ? <hr/> : "" }
                                </div>
                              ))}
                            </div>
                            <br/>

                          </div>
                        ))}
                    </div>
                </div>
                <br/><br/><br/><br/>
                {
                  windowData == null ? "" :
                  <div className="windowBackground" onClick={(e) => setWindowData(null)}>
                    <div className="window" onClick={(e) => e.stopPropagation()}>
                      <div className="right">
                        <a href="#" onClick={(e) => {setWindowData(null); e.preventDefault();}} ><i className="closeButton bi bi-x"></i></a>
                      </div>
                      <div className="windowMainTitle">
                         {windowData.variant.chrom}:{windowData.variant.pos} {windowData.variant.ref}>{windowData.variant.alt}
                      </div>
                      <div className="windowTitle">
                        {windowData.variant.info.info_csq_symbol} {windowData.variant.info[genotypeKey]}
                      </div>
                      <div className="info">{windowData.variant.info.info_csq_symbol} {windowData.variant.info.info_csq_hgvsc}</div>
                      <Annotation annotation={windowData.annotation} actualGenotype={getActualGenotype(windowData.variant, genotypeKey)} />
                    </div><br/><br/><br/><br/>
                  </div>
                }
            </div>
        )
    }

}

function getActualGenotype(variant, genotypeKey) {
  if (variant.ref.localeCompare(variant.alt) > 0) {
    return variant.info[genotypeKey].replace(/\D/g,'').replace("01", "10").replaceAll("0", variant.ref).replaceAll("1", variant.alt);
  }
  return variant.info[genotypeKey].replace(/\D/g,'').replaceAll("0", variant.ref).replaceAll("1", variant.alt);
}

function Loader() {
    return (
        <div className="Loader"><div className="spinner-border spinner-border-sm" role="status"></div> Setting up...</div>
    )
}

function Annotation(props) {
    let annotation = props.annotation;

    return (
      <div className={"annotation annotationOpen"}>
        <div className="annotationTitle">
          <div title="level of evidence (1A best, 4 worst) https://www.pharmgkb.org/page/clinAnnLevels" className={"levelOfEvidence levelOfEvidence" + annotation.pharmGKBAnnotation.levelOfEvidence} style={{fontSize: "15px"}}>{annotation.pharmGKBAnnotation.levelOfEvidence} Level of evidence</div>
          {annotation.pharmGKBAnnotation.drugs.split(";").map(drug => (
            <div className="drug" style={{fontSize: "16px"}}>{drug.toUpperCase()}</div>
          ))}
          <br/>
          <div className="phenotypeCategory">{annotation.pharmGKBAnnotation.phenotypeCategory}</div><br/><br/>
          {annotation.pharmGKBAnnotation.phenotypes == "" ? "" : <div className="phenotypes">{annotation.pharmGKBAnnotation.phenotypes.split(";").map(pt => (<div className="pharmPhenotype">{pt}</div>))}</div>}

        </div>
        <div>
          <b>Evidence count:</b> {annotation.pharmGKBAnnotation.evidenceCount}<br/>
          <b>Last updated:</b> {annotation.pharmGKBAnnotation.latestHistoryDate}<br/>
          <b>Score:</b> {annotation.pharmGKBAnnotation.score}<br/>
          {annotation.pharmGKBAnnotation.specialtyPopulation == null ? "" : <div><b>Specialty population:</b> {annotation.pharmGKBAnnotation.specialtyPopulation}</div> }
          <a href={annotation.pharmGKBAnnotation.url} target="_blank">{annotation.pharmGKBAnnotation.url}</a>

          <div className="alleles">
            <div className="windowTitle">Alleles</div>
            {annotation.pharmGKBAlleles.map(item => (
              <div className={"allele " + (!annotation.pharmGKBAlleles.map(a => a.genotype).includes(props.actualGenotype) ? "alleleAmbiguous " : "") + (props.actualGenotype == item.genotype ? "alleleSelected" : "")}><b>{item.genotype}</b> {item.annotationText} {item.alleleFunction}</div>
            ))}
          </div>
          <div className="evidences">
            <div className="windowTitle">Evidence</div>
            <div className="info">
                <i className="bi bi-info-circle-fill"></i> Individual evidence is scored and used to compute the total level of evidence
            </div><br/>
            {annotation.pharmGKBEvidence.map(item => (
              <div className="evidence">
                <div className="evidenceScore">
                  {item.score.length <= 6 ? item.score : ""}
                </div>
                <div className="evidenceData">
                  <div className="evidenceSummary">
                    {item.summary}
                  </div>
                  <div className="small">{item.evidenceType}</div>
                  <a href={item.evidenceUrl} target="_blank">{item.evidenceUrl}</a><br/>
                  {item.pmid == "" ? "" : <a href={"https://pubmed.ncbi.nlm.nih.gov/" + item.pmid} target="_blank">PubMed {item.pmid}</a>}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    )
}

function LargeLoader() {
    return (
        <div className="LargeLoader"><div className="spinner-border spinner-border-l" role="status"></div><br/>Processing finished. Loading results...</div>
    )
}

function fetchStartProcessing(token, sample) {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(
            {
                token: token,
                sample: sample,
            }
        )
    };

    return fetch(Config.apiBaseUrl + '/pharmacogenomics/startcomputation', requestOptions)
      .then(
        response => {
          return response.text();
        }
      )
      .then(
        data => {
          return data
        }
      );
}

function fetchProgress(token, sample, processId) {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(
            {
                token: token,
                sample: sample,
            }
        )
    };

    return fetch(Config.apiBaseUrl + '/pharmacogenomics/progress/' + processId, requestOptions)
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

function fetchResults(token, sample, processId) {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(
            {
                token: token,
                sample: sample,
            }
        )
    };

    return fetch(Config.apiBaseUrl + '/pharmacogenomics/result/' + processId, requestOptions)
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
