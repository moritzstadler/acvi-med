import React, { useState, useEffect } from 'react';
import './Note.css';

import Config from './config.js';

export default function Note(props) {

    const [notes, setNotes] = useState([]);
    const [notesLoaded, setNotesLoaded] = useState(false);

    useEffect(() => {
        const load = async () => {
          const notes = await fetchNotes(props);
          console.log(notes);
          setNotes(notes);
          setNotesLoaded(true);
        };
        load();
    }, []);

    const deleteNote = (id) => {
      fetchDeleteNote(id, props);
      setNotes(notes.filter(n => n.id != id));
    };

    if (!notesLoaded) {
        return(
            <div className="Note">
                <div className="noteBackground">
                    <div className="noteBox">
                        <LargeLoader/>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="Note">
            <div className="noteBackground">
                <div className="noteBox">
                    <h1>Notes</h1><br/>
                    <div className="info"><i class="bi bi-info-circle-fill"></i> These are all notes you or your colleagues have created on samples you have access to.</div>
                    <hr/>
                    {notes && notes.length > 0 ?
                      <div>
                        {notes.map(n => {
                          return(
                            <div className="singleNote">
                              <div className="noteHeader">
                                <div className="inlineLeft">
                                  <i style={{color: "orange"}} class="bi bi-star-fill"></i> <b><a href={"/view/" + n.sampleName + "/" + n.variantId}>{n.variantPosition}</a></b> | <a href={"/sample/" + n.sampleName}>{n.sampleName}</a>
                                </div>
                                <div className="inlineRight">
                                  {new Date(n.time).toLocaleDateString()} {new Date(n.time).toLocaleTimeString()} | <i onClick={(e) => deleteNote(n.id)} style={{cursor: "pointer"}} class="bi bi-trash"></i>
                                </div>
                              </div>
                              <div className="noteBody">
                                <b>{n.researcherName}</b>:
                                <div className="noteText">
                                  <i>"{n.note}"</i>
                                </div>
                              </div>
                            </div>
                          )
                        })}
                      </div> :
                      <div className="info">It seems you haven't created any notes yet. You can create a note on the detail page of a variant!</div>
                    }
                </div><br/><br/>
            </div>
        </div>
    );
}

function LargeLoader() {
    return (
        <div className="LargeLoader"><div className="spinner-border spinner-border-l" role="status"></div><br/>Loading...</div>
    )
}

function fetchDeleteNote(id, props) {
    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: props.token.tokenString })
    };

    return fetch(Config.apiBaseUrl + '/note/delete/' + id, requestOptions);
}

function fetchNotes(props) {

    const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tokenString: props.token.tokenString })
    };

    return fetch(Config.apiBaseUrl + '/note/getForResearcher', requestOptions)
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