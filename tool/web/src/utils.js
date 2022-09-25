import Config from './config.js';

export function login(callback, email, password) {
	let request = {email: email, password: password};
	send(callback, Config.apiBaseUrl + "/authentication/login", request);
}

export function refresh(callback, tokenString) {
	let request = {tokenString: tokenString};
	send(callback, Config.apiBaseUrl + "/authentication/refresh", request);
}

function send(callback, url, request) {
	const requestOptions = {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request)
    };

	fetch(url, requestOptions)
	.then( response => {
		return response.json();
	})
	.then( data => {
		callback(data);
	}).catch(err=>{callback(null)});
}