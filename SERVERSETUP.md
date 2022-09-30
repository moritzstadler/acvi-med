# Setting up the application on a linux server

This is an introduction to setting up the system on a server for providing it to your university or hospital.

```
ProxyPass /api http://127.0.0.1:8080
ProxyPassReverse /api http://127.0.0.1:8080

ProxyPass / http://127.0.0.1:3000/
ProxyPassReverse / http://127.0.0.1:3000/
```
