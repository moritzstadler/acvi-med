# Setting up the application on a linux server

This is an introduction to setting up the system on a server for providing it to your university or hospital.
These instructions assume your running a linux server for your instituation on the domain `yourdomain.edu`.

`/etc/apache2/sites-available/000-default.conf`

```
<VirtualHost *:80>

        ServerAdmin webmaster@localhost
        DocumentRoot /var/www/html

        ErrorLog ${APACHE_LOG_DIR}/error.log
        CustomLog ${APACHE_LOG_DIR}/access.log combined

        ProxyPass /api http://127.0.0.1:8080
        ProxyPassReverse /api http://127.0.0.1:8080

        ProxyPass / http://127.0.0.1:3000/
        ProxyPassReverse / http://127.0.0.1:3000/

</VirtualHost>

```
Your [config.js file](tool/web/src/config.js) should look like this
```
export const Config = { 
	apiBaseUrl: "http://yourdomain.edu/api",
	appBaseUrl: "http://yourdomain.edu"
};

export default Config;
```
