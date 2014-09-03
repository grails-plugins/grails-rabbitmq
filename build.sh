#!/bin/bash
grails $UPGRADE --non-interactive && grails test-app unit: --non-interactive --stacktrace
