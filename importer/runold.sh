#!/bin/bash
mvn exec:java -Dexec.mainClass="Main" -Dexec.args="$1 $2 $3 $4" -Djdk.tls.client.protocols="TLSv1,TLSv1.1,TLSv1.2"