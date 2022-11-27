# Setting up the application on a Linux server

This is an introduction to setting up the system on a server for providing it to your university or hospital.
These instructions assume your running a linux server for your instituation on the domain `yourdomain.edu`.

Your `/etc/apache2/sites-available/000-default.conf` file should look like this

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

Finally create a http to https redirect witht the following rewrite rule. Open your .htaccess file with any text editor.
<pre><code>sudo vim /var/www/html/.htaccess</code></pre>

Add the following to the .htaccess file
```
RewriteEngine On  
RewriteRule ^(.*)$ https://%{HTTP_HOST}$1 [R=301,L]
RewriteCond %{DOCUMENT_ROOT}%{REQUEST_URI} -f [OR]
RewriteCond %{DOCUMENT_ROOT}%{REQUEST_URI} -d
RewriteRule ^ - [L]
RewriteRule ^ /index.html [L]
```

Finally change the access of the file
<pre><code>sudo chmod 777 .htaccess</code></pre>

And restart apache
<pre><code>sudo systemctl restart apache2</code></pre>
