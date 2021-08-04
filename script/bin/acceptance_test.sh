#!/bin/bash
test $(curl -o /dev/null -s -w "%{http_code}\n" localhost:8765/rest/stock/marketreports) -eq 200
