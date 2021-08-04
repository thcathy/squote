#!/bin/bash
test $(curl -o /dev/null -s -w "%{http_code}\n" ${1}/rest/stock/marketreports) -eq 200
