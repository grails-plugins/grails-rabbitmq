#!/bin/bash
grails clean && grails $UPGRADE --non-interactive && grails test-app unit: --non-interactive --stacktrace
